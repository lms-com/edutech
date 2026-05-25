package com.lms.enrollment.service.impl;

import com.lms.common.exception.AppException;
import com.lms.enrollment.dto.request.QuizSubmitRequest;
import com.lms.enrollment.dto.response.QuizAttemptResponse;
import com.lms.enrollment.dto.response.QuizResultResponse;
import com.lms.enrollment.entity.Enrollment;
import com.lms.enrollment.entity.QuizAttempt;
import com.lms.enrollment.exception.EnrollmentErrorCode;
import com.lms.enrollment.repository.EnrollmentRepository;
import com.lms.enrollment.repository.QuizAttemptRepository;
import com.lms.enrollment.service.QuizAttemptService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class QuizAttemptServiceImpl implements QuizAttemptService {

    EnrollmentRepository enrollmentRepository;
    QuizAttemptRepository quizAttemptRepository;

    @Override
    @Transactional
    public QuizResultResponse submitQuizAttempt(String enrollmentId, String quizId, QuizSubmitRequest request, String userId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new AppException(EnrollmentErrorCode.ENROLLMENT_NOT_FOUND));

        if (!enrollment.getLearnerId().equals(userId)) {
            throw new AppException(EnrollmentErrorCode.UNAUTHORIZED_ACCESS);
        }

        boolean passed = request.getScore() >= 80;

        QuizAttempt attempt = QuizAttempt.builder()
                .id(UUID.randomUUID().toString())
                .enrollment(enrollment)
                .lessonId(quizId)
                .score(request.getScore())
                .isPassed(passed)
                .submittedAt(LocalDateTime.now())
                .build();

        quizAttemptRepository.save(attempt);

        return QuizResultResponse.builder()
                .lessonId(quizId)
                .score(request.getScore())
                .isPassed(passed)
                .feedback(passed ? "Congratulations, you passed!" : "Please try again to get at least 80%")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuizAttemptResponse> getQuizAttempts(String enrollmentId, String quizId, String userId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new AppException(EnrollmentErrorCode.ENROLLMENT_NOT_FOUND));

        if (!enrollment.getLearnerId().equals(userId)) {
            throw new AppException(EnrollmentErrorCode.UNAUTHORIZED_ACCESS);
        }

        return quizAttemptRepository.findAllByEnrollmentIdAndLessonId(enrollmentId, quizId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private QuizAttemptResponse mapToResponse(QuizAttempt attempt) {
        return QuizAttemptResponse.builder()
                .id(attempt.getId())
                .enrollmentId(attempt.getEnrollment().getId())
                .lessonId(attempt.getLessonId())
                .score(attempt.getScore())
                .isPassed(attempt.getIsPassed())
                .submittedAt(attempt.getSubmittedAt())
                .build();
    }
}
