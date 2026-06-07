package com.lms.notification.service;

import com.lms.notification.dto.request.ProgressUpdateRequest;
import com.lms.notification.dto.response.LessonProgressResponse;

import java.util.List;

public interface ProgressService {
    LessonProgressResponse updateLessonProgress(String enrollmentId, String lessonId, ProgressUpdateRequest request, String userId);
    List<LessonProgressResponse> getEnrollmentProgress(String enrollmentId, String userId);
}
