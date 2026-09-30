package com.lms.enrollment.service.impl;

import com.lms.common.dto.response.ApiResponse;
import com.lms.common.exception.AppException;
import com.lms.enrollment.client.CourseServiceClient;
import com.lms.enrollment.client.dto.CourseCorrectAnswerDto;
import com.lms.enrollment.dto.request.QuizAnswerRequest;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.lms.enrollment.entity.LessonProgress;
import com.lms.enrollment.repository.LessonProgressRepository;
import com.lms.enrollment.service.ProgressService;
import org.springframework.data.redis.core.RedisTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class QuizAttemptServiceImpl implements QuizAttemptService {

    EnrollmentRepository enrollmentRepository;
    QuizAttemptRepository quizAttemptRepository;
    LessonProgressRepository lessonProgressRepository;
    ProgressService progressService;
    CourseServiceClient courseServiceClient;
    RedisTemplate<String, Object> redisTemplate;

    /**
     * Ngưỡng đạt tạm dùng chung cho mọi bài. Đúng ra phải lấy `passScore` của chính
     * bài kiểm tra, nhưng API nội bộ correct-answers hiện chưa trả về trường đó.
     */
    static final int DEFAULT_PASS_SCORE = 80;

    /**
     * Nộp bài kiểm tra và CHẤM ĐIỂM Ở SERVER.
     *
     * Client chỉ gửi lựa chọn của mình; đáp án đúng do course-service cung cấp qua
     * API nội bộ. Nhờ vậy điểm số không thể bị sửa từ phía client.
     *
     * @param enrollmentId ID lượt ghi danh
     * @param quizId ID bài kiểm tra (chính là lessonId của bài học loại QUIZ)
     * @param request Danh sách lựa chọn của học viên
     * @param userId ID người dùng đang nộp bài
     */
    @Override
    @Transactional
    public QuizResultResponse submitQuizAttempt(String enrollmentId, String quizId, QuizSubmitRequest request, String userId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new AppException(EnrollmentErrorCode.ENROLLMENT_NOT_FOUND));

        if (!enrollment.getLearnerId().equals(userId)) {
            throw new AppException(EnrollmentErrorCode.UNAUTHORIZED_ACCESS);
        }

        if (request.getAnswers() == null || request.getAnswers().isEmpty()) {
            throw new AppException(EnrollmentErrorCode.QUIZ_ANSWERS_REQUIRED);
        }

        List<CourseCorrectAnswerDto> correctAnswers = fetchCorrectAnswers(quizId);
        if (correctAnswers.isEmpty()) {
            throw new AppException(EnrollmentErrorCode.QUIZ_QUESTIONS_NOT_FOUND);
        }

        // Giữ lựa chọn ĐẦU TIÊN của mỗi câu: client gửi trùng một câu nhiều lần
        // cũng không làm tăng số câu đúng.
        Map<String, String> chosenByQuestion = new LinkedHashMap<>();
        for (QuizAnswerRequest answer : request.getAnswers()) {
            chosenByQuestion.putIfAbsent(answer.getQuestionId(), answer.getAnswerId());
        }

        long correctCount = correctAnswers.stream()
                .filter(question -> {
                    Set<String> accepted = new HashSet<>(
                            question.getCorrectAnswerIds() != null ? question.getCorrectAnswerIds() : List.of());
                    return accepted.contains(chosenByQuestion.get(question.getQuestionId()));
                })
                .count();

        // Mẫu số là TỔNG số câu hỏi, không phải số câu đã trả lời — bỏ trống câu
        // không được làm điểm cao hơn.
        int score = (int) Math.round(correctCount * 100.0 / correctAnswers.size());
        boolean passed = score >= DEFAULT_PASS_SCORE;

        QuizAttempt attempt = QuizAttempt.builder()
                .id(UUID.randomUUID().toString())
                .enrollment(enrollment)
                .lessonId(quizId)
                .score(score)
                .isPassed(passed)
                .submittedAt(LocalDateTime.now())
                .build();

        quizAttemptRepository.save(attempt);

        log.info("Chấm bài quiz {} cho enrollment {}: {}/{} câu đúng, điểm {}, đạt: {}",
                quizId, enrollmentId, correctCount, correctAnswers.size(), score, passed);

        if (passed) {
            LessonProgress progress = lessonProgressRepository.findByEnrollmentIdAndLessonId(enrollmentId, quizId)
                    .orElseGet(() -> LessonProgress.builder()
                            .id(UUID.randomUUID().toString())
                            .enrollment(enrollment)
                            .lessonId(quizId)
                            .isCompleted(false)
                            .lastWatchTimeSeconds(0)
                            .build());
            progress.setIsCompleted(true);
            lessonProgressRepository.save(progress);

            try {
                String redisKey = "progress:" + enrollmentId + ":" + quizId;
                redisTemplate.delete(redisKey);
                redisTemplate.opsForSet().remove("progress:sync_queue", redisKey);
            } catch (Exception e) {
                log.warn("Lỗi dọn Redis cache cho quiz {}: {}", quizId, e.getMessage());
            }

            // Kích hoạt tính lại tiến độ và cấp chứng chỉ nếu đạt 100%
            progressService.recalculateCompletedRate(enrollment);
        }

        return QuizResultResponse.builder()
                .lessonId(quizId)
                .score(score)
                .isPassed(passed)
                .feedback(passed
                        ? "Chúc mừng, bạn đã vượt qua bài kiểm tra!"
                        : String.format("Bạn trả lời đúng %d/%d câu. Vui lòng làm lại để đạt ít nhất %d%% điểm.",
                                correctCount, correctAnswers.size(), DEFAULT_PASS_SCORE))
                .build();
    }

    private List<CourseCorrectAnswerDto> fetchCorrectAnswers(String quizId) {
        ApiResponse<List<CourseCorrectAnswerDto>> response = courseServiceClient.getCorrectAnswers(quizId);
        if (response == null || response.getData() == null) {
            throw new AppException(EnrollmentErrorCode.QUIZ_QUESTIONS_NOT_FOUND);
        }
        return response.getData();
    }

    /**
     * Lấy toàn bộ lịch sử các lần nộp bài kiểm tra (Quiz attempts) của học viên theo bài học cụ thể.
     * Thực hiện xác thực người dùng để tránh truy cập trái phép.
     *
     * @param enrollmentId ID lượt ghi danh
     * @param quizId ID bài kiểm tra
     * @param userId ID người dùng đang yêu cầu
     * @return List<QuizAttemptResponse> Danh sách lịch sử các lần thi thử
     */
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

    /**
     * Chuyển đổi thực thể QuizAttempt sang DTO QuizAttemptResponse.
     */
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
