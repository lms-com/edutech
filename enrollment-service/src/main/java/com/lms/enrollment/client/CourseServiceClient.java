package com.lms.enrollment.client;

import com.lms.common.dto.response.ApiResponse;
import com.lms.enrollment.client.dto.CourseCorrectAnswerDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "course-service")
public interface CourseServiceClient {

    @GetMapping("/api/internal/v1/courses/{courseId}/lesson-count")
    ApiResponse<Object> getLessonCount(@PathVariable("courseId") String courseId);

    @GetMapping("/api/internal/v1/lessons/{lessonId}/validation")
    ApiResponse<Object> validateLesson(@PathVariable("lessonId") String lessonId);

    /** Đáp án đúng của bài kiểm tra, phục vụ chấm điểm ở phía server. */
    @GetMapping("/api/internal/v1/lessons/{lessonId}/correct-answers")
    ApiResponse<List<CourseCorrectAnswerDto>> getCorrectAnswers(@PathVariable("lessonId") String lessonId);
}
