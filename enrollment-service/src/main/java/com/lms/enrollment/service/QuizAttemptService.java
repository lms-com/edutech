package com.lms.enrollment.service;

import com.lms.enrollment.dto.request.QuizSubmitRequest;
import com.lms.enrollment.dto.response.QuizAttemptResponse;
import com.lms.enrollment.dto.response.QuizResultResponse;

import java.util.List;

public interface QuizAttemptService {
    QuizResultResponse submitQuizAttempt(String enrollmentId, String quizId, QuizSubmitRequest request, String userId);
    List<QuizAttemptResponse> getQuizAttempts(String enrollmentId, String quizId, String userId);
}
