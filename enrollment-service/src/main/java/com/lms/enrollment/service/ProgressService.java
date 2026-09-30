package com.lms.enrollment.service;

import com.lms.enrollment.dto.request.ProgressUpdateRequest;
import com.lms.enrollment.dto.response.LessonProgressResponse;

import java.util.List;

public interface ProgressService {
    LessonProgressResponse updateLessonProgress(String enrollmentId, String lessonId, ProgressUpdateRequest request, String userId);
    List<LessonProgressResponse> getEnrollmentProgress(String enrollmentId, String userId);
    void recalculateCompletedRate(com.lms.enrollment.entity.Enrollment enrollment);
}
