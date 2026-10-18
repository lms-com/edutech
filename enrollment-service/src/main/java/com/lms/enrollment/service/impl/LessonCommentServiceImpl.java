package com.lms.enrollment.service.impl;

import com.lms.common.dto.response.ApiResponse;
import com.lms.common.exception.AppException;
import com.lms.enrollment.client.IamServiceClient;
import com.lms.enrollment.dto.request.CreateCommentRequest;
import com.lms.enrollment.dto.response.LearnerInfoResponse;
import com.lms.enrollment.dto.response.LessonCommentResponse;
import com.lms.enrollment.entity.Enrollment;
import com.lms.enrollment.entity.LessonComment;
import com.lms.enrollment.enums.EnrollmentStatus;
import com.lms.enrollment.exception.EnrollmentErrorCode;
import com.lms.enrollment.repository.EnrollmentRepository;
import com.lms.enrollment.repository.LessonCommentRepository;
import com.lms.enrollment.service.LessonCommentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import com.lms.enrollment.config.RabbitMQConfig;
import com.lms.enrollment.client.CourseServiceClient;
import com.lms.enrollment.client.dto.CourseBulkDto;
import com.lms.enrollment.event.LessonCommentEvent;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LessonCommentServiceImpl implements LessonCommentService {

    LessonCommentRepository commentRepository;
    EnrollmentRepository enrollmentRepository;
    IamServiceClient iamServiceClient;
    CourseServiceClient courseServiceClient;
    RabbitTemplate rabbitTemplate;

    @Override
    @Transactional(readOnly = true)
    public Page<LessonCommentResponse> getCommentsByLesson(String lessonId, Pageable pageable) {
        // 1. Lấy trang các bình luận gốc (top-level)
        Page<LessonComment> topCommentsPage = commentRepository
                .findByLessonIdAndParentIdIsNullOrderByCreatedAtDesc(lessonId, pageable);

        if (topCommentsPage.isEmpty()) {
            return Page.empty(pageable);
        }

        List<LessonComment> topComments = topCommentsPage.getContent();
        List<String> topCommentIds = topComments.stream().map(LessonComment::getId).toList();

        // 2. Lấy tất cả replies thuộc về các comment gốc này
        List<LessonComment> allReplies = new ArrayList<>();
        for (String parentId : topCommentIds) {
            allReplies.addAll(commentRepository.findByParentIdOrderByCreatedAtAsc(parentId));
        }

        // 3. Gom toàn bộ userId để query enrich tên/avatar từ IAM
        Set<String> userIds = new HashSet<>();
        topComments.forEach(c -> userIds.add(c.getUserId()));
        allReplies.forEach(c -> userIds.add(c.getUserId()));

        Map<String, LearnerInfoResponse> userMap = fetchUsersInfo(new ArrayList<>(userIds));

        // 4. Group replies theo parentId
        Map<String, List<LessonCommentResponse>> repliesByParentId = allReplies.stream()
                .map(r -> mapToResponse(r, userMap, Collections.emptyList()))
                .collect(Collectors.groupingBy(LessonCommentResponse::getParentId));

        // 5. Map top-level comments kèm replies
        List<LessonCommentResponse> responses = topComments.stream()
                .map(top -> mapToResponse(top, userMap, repliesByParentId.getOrDefault(top.getId(), Collections.emptyList())))
                .toList();

        return new PageImpl<>(responses, pageable, topCommentsPage.getTotalElements());
    }

    @Override
    @Transactional
    public LessonCommentResponse createComment(String courseId, String lessonId, CreateCommentRequest request, String userId) {
        // Kiểm tra quyền: Phải ghi danh vào khóa học này (hoặc là Giảng viên/Admin)
        // Nếu có enrollment, phải ACTIVE
        Optional<Enrollment> enrollmentOpt = enrollmentRepository.findByLearnerIdAndCourseId(userId, courseId);
        if (enrollmentOpt.isPresent() && enrollmentOpt.get().getStatus() != EnrollmentStatus.ACTIVE) {
            throw new AppException(EnrollmentErrorCode.ENROLLMENT_REVOKED, "Quyền học đã bị thu hồi.");
        }

        // Lấy thông tin người bình luận từ IAM
        Map<String, LearnerInfoResponse> userMap = fetchUsersInfo(List.of(userId));
        LearnerInfoResponse userInfo = userMap.get(userId);
        String userName = userInfo != null ? userInfo.getFullName() : "Học viên";
        String userAvatar = userInfo != null ? userInfo.getAvatarUrl() : null;

        LessonComment comment = LessonComment.builder()
                .lessonId(lessonId)
                .courseId(courseId)
                .userId(userId)
                .userName(userName)
                .userAvatar(userAvatar)
                .parentId(request.getParentId())
                .content(request.getContent().trim())
                .likesCount(0)
                .build();

        LessonComment saved = commentRepository.save(comment);
        log.info("💬 Tạo bình luận mới [{}] tại bài học [{}] bởi user [{}]", saved.getId(), lessonId, userId);

        // Bắn sự kiện thông báo thời gian thực (Real-time Q&A Notification)
        try {
            String shortContent = comment.getContent().length() > 80
                    ? comment.getContent().substring(0, 80) + "..."
                    : comment.getContent();

            if (request.getParentId() == null) {
                // Câu hỏi mới của học viên -> Gửi thông báo cho Giảng viên
                List<CourseBulkDto> bulk = courseServiceClient.getCourseBulk(List.of(courseId));
                if (bulk != null && !bulk.isEmpty()) {
                    CourseBulkDto courseInfo = bulk.get(0);
                    String instructorId = courseInfo.getInstructorId();
                    if (instructorId != null && !instructorId.equals(userId)) {
                        LessonCommentEvent event = LessonCommentEvent.builder()
                                .recipientId(instructorId)
                                .senderId(userId)
                                .senderName(userName)
                                .title("Câu hỏi mới trong khóa học: " + courseInfo.getTitle())
                                .content(userName + ": \"" + shortContent + "\"")
                                .courseId(courseId)
                                .lessonId(lessonId)
                                .commentId(saved.getId())
                                .build();
                        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_ENROLLMENT, RabbitMQConfig.ROUTING_KEY_COMMENT, event);
                        log.info("✔ Đã phát thông báo câu hỏi mới tới giảng viên [{}]", instructorId);
                    }
                }
            } else {
                // Phản hồi câu hỏi -> Gửi thông báo cho tác giả của câu hỏi gốc
                commentRepository.findById(request.getParentId()).ifPresent(parent -> {
                    if (!parent.getUserId().equals(userId)) {
                        LessonCommentEvent event = LessonCommentEvent.builder()
                                .recipientId(parent.getUserId())
                                .senderId(userId)
                                .senderName(userName)
                                .title(userName + " đã trả lời câu hỏi của bạn")
                                .content("\"" + shortContent + "\"")
                                .courseId(courseId)
                                .lessonId(lessonId)
                                .commentId(saved.getId())
                                .build();
                        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_ENROLLMENT, RabbitMQConfig.ROUTING_KEY_COMMENT, event);
                        log.info("✔ Đã phát thông báo phản hồi tới học viên [{}]", parent.getUserId());
                    }
                });
            }
        } catch (Exception e) {
            log.warn("Không thể gửi sự kiện thông báo Q&A qua RabbitMQ: {}", e.getMessage());
        }

        return mapToResponse(saved, userMap, Collections.emptyList());
    }

    @Override
    @Transactional
    public void deleteComment(String commentId, String userId) {
        LessonComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new AppException(EnrollmentErrorCode.COMMENT_NOT_FOUND, "Không tìm thấy bình luận."));

        if (!comment.getUserId().equals(userId)) {
            throw new AppException(EnrollmentErrorCode.UNAUTHORIZED_ACCESS, "Bạn không có quyền xóa bình luận này.");
        }

        comment.setIsDeleted(true);
        commentRepository.save(comment);

        // Xóa luôn các reply con nếu có
        List<LessonComment> replies = commentRepository.findByParentIdOrderByCreatedAtAsc(commentId);
        for (LessonComment reply : replies) {
            reply.setIsDeleted(true);
        }
        commentRepository.saveAll(replies);

        log.info("🗑️ Đã xóa bình luận [{}] bởi user [{}]", commentId, userId);
    }

    @Override
    @Transactional
    public LessonCommentResponse likeComment(String commentId) {
        LessonComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new AppException(EnrollmentErrorCode.COMMENT_NOT_FOUND, "Không tìm thấy bình luận."));

        comment.setLikesCount(comment.getLikesCount() + 1);
        LessonComment saved = commentRepository.save(comment);

        Map<String, LearnerInfoResponse> userMap = fetchUsersInfo(List.of(saved.getUserId()));
        return mapToResponse(saved, userMap, Collections.emptyList());
    }

    // ================== Helper Methods ==================

    private Map<String, LearnerInfoResponse> fetchUsersInfo(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) return Collections.emptyMap();
        try {
            ApiResponse<List<LearnerInfoResponse>> res = iamServiceClient.getUsersByIds(userIds);
            if (res != null && res.getData() != null) {
                return res.getData().stream()
                        .filter(u -> u.getId() != null)
                        .collect(Collectors.toMap(LearnerInfoResponse::getId, u -> u, (a, b) -> a));
            }
        } catch (Exception e) {
            log.warn("⚠️ Không thể lấy thông tin user từ IAM: {}", e.getMessage());
        }
        return Collections.emptyMap();
    }

    private LessonCommentResponse mapToResponse(
            LessonComment c,
            Map<String, LearnerInfoResponse> userMap,
            List<LessonCommentResponse> replies
    ) {
        LearnerInfoResponse info = userMap.get(c.getUserId());
        String name = (info != null && info.getFullName() != null) ? info.getFullName() : (c.getUserName() != null ? c.getUserName() : "Học viên");
        String avatar = (info != null && info.getAvatarUrl() != null) ? info.getAvatarUrl() : c.getUserAvatar();

        return LessonCommentResponse.builder()
                .id(c.getId())
                .lessonId(c.getLessonId())
                .courseId(c.getCourseId())
                .userId(c.getUserId())
                .userName(name)
                .userAvatar(avatar)
                .parentId(c.getParentId())
                .content(c.getContent())
                .likesCount(c.getLikesCount())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .replies(replies)
                .build();
    }
}
