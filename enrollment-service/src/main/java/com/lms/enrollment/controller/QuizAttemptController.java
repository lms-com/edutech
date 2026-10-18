package com.lms.enrollment.controller;

import com.lms.common.dto.response.ApiResponse;
import com.lms.enrollment.dto.request.QuizSubmitRequest;
import com.lms.enrollment.dto.response.QuizAttemptResponse;
import com.lms.enrollment.dto.response.QuizResultResponse;
import com.lms.enrollment.service.QuizAttemptService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/quiz-attempts")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class QuizAttemptController {

    QuizAttemptService quizAttemptService;

    @PostMapping("/enrollments/{enrollmentId}/quizzes/{quizId}")
    public ApiResponse<QuizResultResponse> submitQuizAttempt(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String enrollmentId,
            @PathVariable String quizId,
            @Valid @RequestBody QuizSubmitRequest request) {
        return ApiResponse.success(quizAttemptService.submitQuizAttempt(enrollmentId, quizId, request, userId));
    }

    @GetMapping("/enrollments/{enrollmentId}/quizzes/{quizId}")
    public ApiResponse<List<QuizAttemptResponse>> getQuizAttempts(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String enrollmentId,
            @PathVariable String quizId) {
        return ApiResponse.success(quizAttemptService.getQuizAttempts(enrollmentId, quizId, userId));
    }
}
