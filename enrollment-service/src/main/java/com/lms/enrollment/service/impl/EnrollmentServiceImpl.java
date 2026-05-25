package com.lms.enrollment.service.impl;

import com.lms.common.exception.AppException;
import com.lms.enrollment.dto.event.OrderCompletedEvent;
import com.lms.enrollment.dto.response.EnrollmentDetailResponse;
import com.lms.enrollment.dto.response.EnrollmentResponse;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class EnrollmentServiceImpl implements EnrollmentService {

    EnrollmentRepository enrollmentRepository;

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

    @Override
    @Transactional(readOnly = true)
    public EnrollmentDetailResponse getEnrollmentDetail(String enrollmentId, String userId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new AppException(EnrollmentErrorCode.ENROLLMENT_NOT_FOUND));

        if (!enrollment.getLearnerId().equals(userId)) {
            throw new AppException(EnrollmentErrorCode.UNAUTHORIZED_ACCESS);
        }

        return EnrollmentDetailResponse.builder()
                .id(enrollment.getId())
                .courseId(enrollment.getCourseId())
                .learnerId(enrollment.getLearnerId())
                .status(enrollment.getStatus())
                .startedAt(enrollment.getStartedAt())
                .completedRate(enrollment.getCompletedRate())
                .progresses(new ArrayList<>())
                .quizAttempts(new ArrayList<>())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EnrollmentResponse> getMyEnrollments(String learnerId, Pageable pageable) {
        return enrollmentRepository.findAllByLearnerId(learnerId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional
    public void completeEnrollment(String enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new AppException(EnrollmentErrorCode.ENROLLMENT_NOT_FOUND));

        enrollment.setCompletedRate(100);
        enrollmentRepository.save(enrollment);
    }

    @Override
    @Transactional
    public void enrollFromOrder(OrderCompletedEvent event) {
        List<Enrollment> enrollments = event.getCourseIds().stream()
            .map(courseId -> Enrollment.builder()
                .learnerId(event.getLearnerId())
                .courseId(courseId)
                .status(EnrollmentStatus.ACTIVE)
                .completedRate(0)
                .build())
            .toList();

        enrollmentRepository.saveAll(enrollments);

        log.info("Ghi danh thành công {} khóa học cho learnerId={}",
            enrollments.size(), event.getLearnerId());
    }

    @Override
    @Transactional
    public void revokeEnrollment(String enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new AppException(EnrollmentErrorCode.ENROLLMENT_NOT_FOUND));

        enrollment.setStatus(EnrollmentStatus.REVOKED);
        enrollmentRepository.save(enrollment);
        log.info("Thu hồi thành công quyền truy cập ghi danh ID: {}", enrollmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public EnrollmentResponse getEnrollment(String learnerId, String courseId) {
        return enrollmentRepository.findByLearnerIdAndCourseId(learnerId, courseId)
                .map(this::mapToResponse)
                .orElse(null);
    }

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
