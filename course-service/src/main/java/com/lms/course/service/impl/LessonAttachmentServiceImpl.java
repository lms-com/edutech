package com.lms.course.service.impl;

import com.lms.common.exception.AppException;
import com.lms.course.dto.request.LessonAttachmentRequest;
import com.lms.course.dto.response.LessonAttachmentResponse;
import com.lms.course.entity.Lesson;
import com.lms.course.entity.LessonAttachment;
import com.lms.course.exception.CourseErrorCode;
import com.lms.course.repository.LessonAttachmentRepository;
import com.lms.course.repository.LessonRepository;
import com.lms.course.service.LessonAttachmentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LessonAttachmentServiceImpl implements LessonAttachmentService {

    LessonAttachmentRepository attachmentRepository;
    LessonRepository lessonRepository;

    @Override
    @Transactional(readOnly = true)
    public List<LessonAttachmentResponse> getAttachments(String lessonId) {
        // Kiểm tra bài học tồn tại
        if (!lessonRepository.existsById(lessonId)) {
            throw new AppException(CourseErrorCode.LESSON_NOT_FOUND);
        }

        return attachmentRepository.findByLessonIdOrderByCreatedAtAsc(lessonId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public LessonAttachmentResponse addAttachment(String lessonId, LessonAttachmentRequest request) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new AppException(CourseErrorCode.LESSON_NOT_FOUND));

        LessonAttachment attachment = LessonAttachment.builder()
                .lesson(lesson)
                .name(request.getName().trim())
                .fileUrl(request.getFileUrl().trim())
                .fileSize(request.getFileSize() != null ? request.getFileSize().trim() : null)
                .fileType(request.getFileType() != null ? request.getFileType().trim().toUpperCase() : null)
                .build();

        LessonAttachment saved = attachmentRepository.save(attachment);
        log.info("✔ [Course] Đã thêm tài liệu [{}] vào bài học [{}]", saved.getName(), lessonId);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void deleteAttachment(String attachmentId) {
        LessonAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new AppException(CourseErrorCode.ATTACHMENT_NOT_FOUND));

        attachmentRepository.delete(attachment);
        log.info("✔ [Course] Đã xóa tài liệu đính kèm [{}]", attachmentId);
    }

    private LessonAttachmentResponse mapToResponse(LessonAttachment entity) {
        return LessonAttachmentResponse.builder()
                .id(entity.getId())
                .lessonId(entity.getLesson() != null ? entity.getLesson().getId() : null)
                .name(entity.getName())
                .fileUrl(entity.getFileUrl())
                .fileSize(entity.getFileSize())
                .fileType(entity.getFileType())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
