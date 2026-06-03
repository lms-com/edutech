package com.lms.enrollment.service.impl;

import com.lms.common.exception.AppException;
import com.lms.enrollment.dto.event.OrderCompletedEvent;
import com.lms.enrollment.dto.response.EnrollmentDetailResponse;
import com.lms.enrollment.dto.response.EnrollmentResponse;
import com.lms.enrollment.dto.response.LessonProgressResponse;
import com.lms.enrollment.dto.response.QuizAttemptResponse;
import com.lms.enrollment.dto.response.EnrollmentValidationResponse;
import com.lms.enrollment.entity.Enrollment;
import com.lms.enrollment.enums.EnrollmentStatus;
import com.lms.enrollment.exception.EnrollmentErrorCode;
import com.lms.enrollment.repository.EnrollmentRepository;
import com.lms.enrollment.service.EnrollmentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lms.enrollment.client.IamServiceClient;
import com.lms.enrollment.dto.response.LearnerInfoResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class EnrollmentServiceImpl implements EnrollmentService {

    EnrollmentRepository enrollmentRepository;
    IamServiceClient iamServiceClient;

    /**
     * Đăng ký khóa học cho học viên (Learner) một cách trực tiếp.
     * Kiểm tra xem học viên đã đăng ký khóa học này chưa để tránh đăng ký trùng lặp.
     *
     * @param learnerId ID của học viên
     * @param courseId ID của khóa học
     * @return EnrollmentResponse Thông tin lượt đăng ký học vừa tạo
     */
    @Override
    @Transactional
    public EnrollmentResponse enrollLearner(String learnerId, String courseId) {
        if (enrollmentRepository.existsByLearnerIdAndCourseId(learnerId, courseId)) {
            throw new AppException(EnrollmentErrorCode.ALREADY_ENROLLED);
        }

        Enrollment enrollment = Enrollment.builder()
                .id(UUID.randomUUID().toString())
                .courseId(courseId)
                .learnerId(learnerId)
                .status(EnrollmentStatus.ACTIVE)
                .startedAt(LocalDateTime.now())
                .completedRate(0)
                .build();

        enrollment = enrollmentRepository.save(enrollment);

        return mapToResponse(enrollment);
    }

    /**
     * Lấy chi tiết thông tin ghi danh của học viên bao gồm tiến độ học tập và lịch sử làm bài kiểm tra (quiz).
     * Chỉ cho phép chính học viên đó truy cập thông tin ghi danh của mình để bảo mật.
     *
     * @param enrollmentId ID lượt ghi danh
     * @param userId ID người dùng đang gửi yêu cầu
     * @return EnrollmentDetailResponse Chi tiết ghi danh kèm danh sách tiến độ và bài thi thử
     */
    @Override
    @Transactional(readOnly = true)
    public EnrollmentDetailResponse getEnrollmentDetail(String enrollmentId, String userId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new AppException(EnrollmentErrorCode.ENROLLMENT_NOT_FOUND));

        if (!enrollment.getLearnerId().equals(userId)) {
            throw new AppException(EnrollmentErrorCode.UNAUTHORIZED_ACCESS);
        }

        List<LessonProgressResponse> progressResponses = enrollment.getProgresses().stream()
                .map(p -> LessonProgressResponse.builder()
                        .id(p.getId())
                        .enrollmentId(enrollment.getId())
                        .lessonId(p.getLessonId())
                        .isCompleted(p.getIsCompleted())
                        .lastWatchTimeSeconds(p.getLastWatchTimeSeconds())
                        .build())
                .toList();

        List<QuizAttemptResponse> quizAttemptResponses = enrollment.getQuizAttempts().stream()
                .map(q -> QuizAttemptResponse.builder()
                        .id(q.getId())
                        .enrollmentId(enrollment.getId())
                        .lessonId(q.getLessonId())
                        .score(q.getScore())
                        .isPassed(q.getIsPassed())
                        .submittedAt(q.getSubmittedAt())
                        .build())
                .toList();

        return EnrollmentDetailResponse.builder()
                .id(enrollment.getId())
                .courseId(enrollment.getCourseId())
                .learnerId(enrollment.getLearnerId())
                .status(enrollment.getStatus())
                .startedAt(enrollment.getStartedAt())
                .completedRate(enrollment.getCompletedRate())
                .progresses(progressResponses)
                .quizAttempts(quizAttemptResponses)
                .build();
    }

    /**
     * Lấy danh sách các khóa học đã ghi danh của học viên hiện tại hỗ trợ phân trang.
     *
     * @param learnerId ID học viên
     * @param pageable Cấu hình phân trang
     * @return Page<EnrollmentResponse> Trang danh sách các khóa học đã ghi danh
     */
    @Override
    @Transactional(readOnly = true)
    public Page<EnrollmentResponse> getMyEnrollments(String learnerId, Pageable pageable) {
        return enrollmentRepository.findAllByLearnerId(learnerId, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Đánh dấu hoàn thành khóa học (đặt tỷ lệ hoàn thành completedRate = 100%).
     *
     * @param enrollmentId ID lượt ghi danh
     */
    @Override
    @Transactional
    public void completeEnrollment(String enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new AppException(EnrollmentErrorCode.ENROLLMENT_NOT_FOUND));

        enrollment.setCompletedRate(100);
        enrollmentRepository.save(enrollment);
    }

    /**
     * Tự động đăng ký khóa học khi nhận được sự kiện thanh toán đơn hàng thành công từ RabbitMQ (OrderCompletedEvent).
     * Đảm bảo tính Idempotent để tránh đăng ký trùng lặp nếu sự kiện được gửi nhiều lần.
     *
     * @param event Sự kiện đơn hàng hoàn tất chứa danh sách khóa học và ID học viên
     */
    @Override
    @Transactional
    public void enrollFromOrder(OrderCompletedEvent event) {
        log.info("Processing auto-enrollment for learnerId: {} and courses: {}", event.getLearnerId(), event.getCourseIds());
        for (String courseId : event.getCourseIds()) {
            try {
                if (enrollmentRepository.existsByLearnerIdAndCourseId(event.getLearnerId(), courseId)) {
                    log.info("Learner {} is already enrolled in course {}, skipping.", event.getLearnerId(), courseId);
                    continue;
                }
                Enrollment enrollment = Enrollment.builder()
                        .id(UUID.randomUUID().toString())
                        .learnerId(event.getLearnerId())
                        .courseId(courseId)
                        .status(EnrollmentStatus.ACTIVE)
                        .completedRate(0)
                        .build();
                enrollmentRepository.save(enrollment);
                log.info("Enrolled learner {} to course {} successfully.", event.getLearnerId(), courseId);
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                log.warn("Double-enrollment detected via database unique constraint for learnerId: {} and courseId: {}, ignoring: {}", 
                        event.getLearnerId(), courseId, e.getMessage());
            } catch (Exception e) {
                log.error("Failed to enroll learner {} to course {}: {}", event.getLearnerId(), courseId, e.getMessage());
                throw e;
            }
        }
    }

    /**
     * Thu hồi quyền truy cập khóa học của học viên (Chuyển trạng thái sang REVOKED).
     *
     * @param enrollmentId ID lượt ghi danh
     */
    @Override
    @Transactional
    public void revokeEnrollment(String enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new AppException(EnrollmentErrorCode.ENROLLMENT_NOT_FOUND));

        enrollment.setStatus(EnrollmentStatus.REVOKED);
        enrollmentRepository.save(enrollment);
        log.info("Thu hồi thành công quyền truy cập ghi danh ID: {}", enrollmentId);
    }

    /**
     * Lấy thông tin ghi danh đơn lẻ dựa vào ID học viên và ID khóa học.
     *
     * @param learnerId ID học viên
     * @param courseId ID khóa học
     * @return EnrollmentResponse Thông tin ghi danh hoặc null nếu chưa đăng ký
     */
    @Override
    @Transactional(readOnly = true)
    public EnrollmentResponse getEnrollment(String learnerId, String courseId) {
        return enrollmentRepository.findByLearnerIdAndCourseId(learnerId, courseId)
                .map(this::mapToResponse)
                .orElse(null);
    }

    /**
     * Xác thực quyền truy cập khóa học của học viên (API nội bộ gọi từ microservice khác).
     * Kiểm tra trạng thái ACTIVE và đảm bảo bản ghi chưa bị xóa mềm.
     *
     * @param learnerId ID học viên
     * @param courseId ID khóa học
     * @return EnrollmentValidationResponse Kết quả kiểm tra quyền truy cập và trạng thái cụ thể
     */
    @Override
    @Transactional(readOnly = true)
    public EnrollmentValidationResponse validateAccess(String learnerId, String courseId) {
        return enrollmentRepository
            .findByLearnerIdAndCourseId(learnerId, courseId)
            .map(enrollment -> EnrollmentValidationResponse.builder()
                .hasAccess(enrollment.getStatus() == EnrollmentStatus.ACTIVE)
                .enrollmentStatus(enrollment.getStatus().name())
                .build())
            .orElse(EnrollmentValidationResponse.builder()
                .hasAccess(false)
                .enrollmentStatus("NOT_FOUND")
                .build());
    }

    /**
     * Lấy danh sách học viên tham gia một khóa học cụ thể dành cho giảng viên (Instructor).
     * Kết hợp gọi IamServiceClient theo cơ chế BATCH để lấy thông tin cá nhân (Họ tên, avatar)
     * nhằm tối ưu hóa hiệu năng, giải quyết triệt để vấn đề N+1 truy vấn và được bảo vệ bởi Circuit Breaker.
     *
     * @param courseId ID khóa học
     * @param pageable Cấu hình phân trang
     * @return Page<EnrollmentResponse> Trang danh sách ghi danh kèm thông tin chi tiết học viên
     */
    @Override
    @Transactional(readOnly = true)
    public Page<EnrollmentResponse> getCourseEnrollments(String courseId, Pageable pageable) {
        Page<Enrollment> enrollments = enrollmentRepository.findByCourseId(courseId, pageable);

        List<String> learnerIds = enrollments.stream()
            .map(Enrollment::getLearnerId)
            .distinct()
            .toList();

        Map<String, LearnerInfoResponse> learnerMap;
        try {
            var response = iamServiceClient.getUsersByIds(learnerIds);
            if (response != null && response.getData() != null) {
                learnerMap = response.getData().stream()
                    .collect(Collectors.toMap(LearnerInfoResponse::getId, l -> l, (l1, l2) -> l1));
            } else {
                learnerMap = Map.of();
            }
        } catch (Exception e) {
            log.error("Failed to fetch learner details from iam-service for learnerIds: {}. Error: {}", learnerIds, e.getMessage());
            learnerMap = Map.of();
        }

        final Map<String, LearnerInfoResponse> finalLearnerMap = learnerMap;

        return enrollments.map(enrollment -> {
            LearnerInfoResponse learner = finalLearnerMap.getOrDefault(
                enrollment.getLearnerId(),
                LearnerInfoResponse.builder().fullName("Học viên").build()
            );
            return EnrollmentResponse.builder()
                .id(enrollment.getId())
                .courseId(enrollment.getCourseId())
                .learnerId(enrollment.getLearnerId())
                .learnerName(learner.getFullName())
                .learnerAvatar(learner.getAvatarUrl())
                .completedRate(enrollment.getCompletedRate())
                .status(enrollment.getStatus())
                .startedAt(enrollment.getStartedAt())
                .build();
        });
    }

    /**
     * Chuyển đổi đối tượng thực thể Enrollment sang dữ liệu trả về EnrollmentResponse DTO.
     */
    private EnrollmentResponse mapToResponse(Enrollment enrollment) {
        return EnrollmentResponse.builder()
                .id(enrollment.getId())
                .courseId(enrollment.getCourseId())
                .learnerId(enrollment.getLearnerId())
                .status(enrollment.getStatus())
                .startedAt(enrollment.getStartedAt())
                .completedRate(enrollment.getCompletedRate())
                .build();
    }
}
