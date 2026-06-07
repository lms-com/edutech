package com.lms.notification.service;

import com.lms.notification.dto.request.QuizSubmitRequest;
import com.lms.notification.dto.response.QuizAttemptResponse;
import com.lms.notification.dto.response.QuizResultResponse;

import java.util.List;

public interface QuizAttemptService {
    QuizResultResponse submitQuizAttempt(String enrollmentId, String quizId, QuizSubmitRequest request, String userId);
    List<QuizAttemptResponse> getQuizAttempts(String enrollmentId, String quizId, String userId);
}
