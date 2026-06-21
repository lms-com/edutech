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

    /**
     * Nộp bài kiểm tra trắc nghiệm (Quiz).
     * Thực hiện kiểm tra tính hợp lệ và phân quyền của học viên đối với lượt ghi danh.
     * Tự động tính toán kết quả ĐẠT (Passed) nếu điểm số đạt từ 80% trở lên.
     * Lưu trữ chi tiết bài nộp của học viên vào MySQL database.
     *
     * @param enrollmentId ID lượt ghi danh
     * @param quizId ID bài kiểm tra (tương ứng với lessonId dạng quiz)
     * @param request Điểm số và thông tin nộp bài từ người dùng
     * @param userId ID người dùng gửi yêu cầu
     * @return QuizResultResponse Kết quả chấm điểm kèm phản hồi đánh giá
     */
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
                .feedback(passed ? "Chúc mừng, bạn đã vượt qua bài kiểm tra!" : "Vui lòng làm lại bài để đạt ít nhất 80% điểm số yêu cầu")
                .build();
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
