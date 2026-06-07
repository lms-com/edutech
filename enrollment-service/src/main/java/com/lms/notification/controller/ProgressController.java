package com.lms.notification.controller;

import com.lms.common.dto.response.ApiResponse;
import com.lms.notification.dto.request.ProgressUpdateRequest;
import com.lms.notification.dto.response.LessonProgressResponse;
import com.lms.notification.service.ProgressService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/progress")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProgressController {

    ProgressService progressService;

    @PutMapping("/enrollments/{enrollmentId}/lessons/{lessonId}")
    public ApiResponse<LessonProgressResponse> updateLessonProgress(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String enrollmentId,
            @PathVariable String lessonId,
            @Valid @RequestBody ProgressUpdateRequest request) {
        return ApiResponse.success(progressService.updateLessonProgress(enrollmentId, lessonId, request, userId));
    }

    @GetMapping("/enrollments/{enrollmentId}")
    public ApiResponse<List<LessonProgressResponse>> getEnrollmentProgress(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String enrollmentId) {
        return ApiResponse.success(progressService.getEnrollmentProgress(enrollmentId, userId));
    }
}
