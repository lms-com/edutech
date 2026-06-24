package com.lms.enrollment.client;

import com.lms.common.dto.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "course-service")
public interface CourseServiceClient {

    @GetMapping("/api/internal/v1/courses/{courseId}/lesson-count")
    ApiResponse<Object> getLessonCount(@PathVariable("courseId") String courseId);

    @GetMapping("/api/internal/v1/lessons/{lessonId}/validation")
    ApiResponse<Object> validateLesson(@PathVariable("lessonId") String lessonId);
}
