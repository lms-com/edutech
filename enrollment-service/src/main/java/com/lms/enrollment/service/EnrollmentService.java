package com.lms.enrollment.service;

import com.lms.enrollment.dto.response.EnrollmentDetailResponse;
import com.lms.enrollment.dto.response.EnrollmentResponse;
import com.lms.enrollment.dto.response.EnrollmentValidationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface EnrollmentService {
    EnrollmentResponse enrollLearner(String learnerId, String courseId);
    EnrollmentDetailResponse getEnrollmentDetail(String enrollmentId, String userId);
    Page<EnrollmentResponse> getMyEnrollments(String learnerId, Pageable pageable);
    void completeEnrollment(String enrollmentId);
    void enrollFromOrder(com.lms.enrollment.dto.event.OrderCompletedEvent event);
    void revokeEnrollment(String enrollmentId);
    EnrollmentResponse getEnrollment(String learnerId, String courseId);
    EnrollmentValidationResponse validateAccess(String learnerId, String courseId);
    Page<EnrollmentResponse> getCourseEnrollments(String courseId, Pageable pageable);
}
