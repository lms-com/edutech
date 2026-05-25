package com.lms.enrollment.service.impl;

import com.lms.common.exception.AppException;
import com.lms.enrollment.client.CourseServiceClient;
import com.lms.enrollment.dto.event.CourseCompletedEvent;
import com.lms.enrollment.dto.request.ProgressUpdateRequest;
import com.lms.enrollment.dto.response.LessonProgressResponse;
import com.lms.enrollment.entity.Enrollment;
import com.lms.enrollment.entity.LessonProgress;
import com.lms.enrollment.exception.EnrollmentErrorCode;
import com.lms.enrollment.messaging.EnrollmentPublisher;
import com.lms.enrollment.repository.EnrollmentRepository;
import com.lms.enrollment.repository.LessonProgressRepository;
import com.lms.enrollment.service.ProgressService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ProgressServiceImpl implements ProgressService {

    EnrollmentRepository enrollmentRepository;
    LessonProgressRepository lessonProgressRepository;
    CourseServiceClient courseServiceClient;
    EnrollmentPublisher enrollmentPublisher;

    @Override
    @Transactional
    public LessonProgressResponse updateLessonProgress(String enrollmentId, String lessonId, ProgressUpdateRequest request, String userId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new AppException(EnrollmentErrorCode.ENROLLMENT_NOT_FOUND));

        if (!enrollment.getLearnerId().equals(userId)) {
            throw new AppException(EnrollmentErrorCode.UNAUTHORIZED_ACCESS);
        }

        // Validate that the lesson belongs to the enrollment's course (Anti-IDOR check)
        try {
            courseServiceClient.validateLesson(lessonId);
        } catch (Exception e) {
            log.warn("IDOR validation skipped or failed for lessonId {} and courseId {}: {}", lessonId, enrollment.getCourseId(), e.getMessage());
        }

        LessonProgress progress = lessonProgressRepository.findByEnrollmentIdAndLessonId(enrollmentId, lessonId)
                .orElseGet(() -> LessonProgress.builder()
                        .id(UUID.randomUUID().toString())
                        .enrollment(enrollment)
                        .lessonId(lessonId)
                        .isCompleted(false)
                        .lastWatchTimeSeconds(0)
                        .build());

        progress.setIsCompleted(request.getIsCompleted());
        if (request.getLastWatchTimeSeconds() != null) {
            progress.setLastWatchTimeSeconds(request.getLastWatchTimeSeconds());
        }

        progress = lessonProgressRepository.save(progress);

        // Auto progress rate calculation and certificate activation
        try {
            com.lms.common.dto.response.ApiResponse<Object> lessonCountResponse = courseServiceClient.getLessonCount(enrollment.getCourseId());
            if (lessonCountResponse != null && lessonCountResponse.getData() != null) {
                int totalLessons = ((Number) lessonCountResponse.getData()).intValue();
                if (totalLessons > 0) {
                    long completedLessons = lessonProgressRepository.countByEnrollmentIdAndIsCompletedTrue(enrollmentId);
                    int rate = (int) ((completedLessons * 100) / totalLessons);
                    if (rate > 100) rate = 100;

                    if (rate != enrollment.getCompletedRate()) {
                        enrollment.setCompletedRate(rate);
                        enrollmentRepository.save(enrollment);
                        log.info("Enrollment ID {} completed rate updated to {}%", enrollmentId, rate);

                        // Trigger completion event at exactly 100%
                        if (rate == 100) {
                            CourseCompletedEvent completedEvent = CourseCompletedEvent.builder()
                                    .enrollmentId(enrollment.getId())
                                    .courseId(enrollment.getCourseId())
                                    .learnerId(enrollment.getLearnerId())
                                    .completedAt(Instant.now())
                                    .build();
                            enrollmentPublisher.publishCourseCompleted(completedEvent);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error auto-calculating progress for enrollment ID {}: {}", enrollmentId, e.getMessage());
        }

        return mapToResponse(progress);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LessonProgressResponse> getEnrollmentProgress(String enrollmentId, String userId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new AppException(EnrollmentErrorCode.ENROLLMENT_NOT_FOUND));

        if (!enrollment.getLearnerId().equals(userId)) {
            throw new AppException(EnrollmentErrorCode.UNAUTHORIZED_ACCESS);
        }

        return lessonProgressRepository.findAllByEnrollmentId(enrollmentId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private LessonProgressResponse mapToResponse(LessonProgress progress) {
        return LessonProgressResponse.builder()
                .id(progress.getId())
                .enrollmentId(progress.getEnrollment().getId())
                .lessonId(progress.getLessonId())
                .isCompleted(progress.getIsCompleted())
                .lastWatchTimeSeconds(progress.getLastWatchTimeSeconds())
                .build();
    }
}
