package com.lms.enrollment.service;

import com.lms.common.dto.response.ApiResponse;
import com.lms.enrollment.client.CourseServiceClient;
import com.lms.enrollment.client.dto.CourseCorrectAnswerDto;
import com.lms.enrollment.dto.request.QuizAnswerRequest;
import com.lms.enrollment.dto.request.QuizSubmitRequest;
import com.lms.enrollment.dto.response.QuizResultResponse;
import com.lms.enrollment.entity.Enrollment;
import com.lms.enrollment.entity.LessonProgress;
import com.lms.enrollment.entity.QuizAttempt;
import com.lms.enrollment.repository.EnrollmentRepository;
import com.lms.enrollment.repository.LessonProgressRepository;
import com.lms.enrollment.repository.QuizAttemptRepository;
import com.lms.enrollment.service.impl.QuizAttemptServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizAttemptServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private QuizAttemptRepository quizAttemptRepository;

    @Mock
    private LessonProgressRepository lessonProgressRepository;

    @Mock
    private ProgressService progressService;

    @Mock
    private CourseServiceClient courseServiceClient;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private SetOperations<String, Object> setOperations;

    @InjectMocks
    private QuizAttemptServiceImpl quizAttemptService;

    private Enrollment enrollment;

    @BeforeEach
    void setUp() {
        enrollment = Enrollment.builder()
                .id("enr-quiz-1")
                .courseId("course-100")
                .learnerId("learner-1")
                .completedRate(50)
                .build();
    }

    @Test
    @DisplayName("Nộp bài Quiz đạt điểm (100%) -> Lưu attempt, đánh dấu hoàn thành LessonProgress và gọi tính lại tiến độ")
    void submitQuiz_passed_marksProgressCompleted() {
        when(enrollmentRepository.findById("enr-quiz-1")).thenReturn(Optional.of(enrollment));

        // Giả lập 2 câu hỏi đáp án đúng từ course-service
        CourseCorrectAnswerDto q1 = new CourseCorrectAnswerDto();
        q1.setQuestionId("q1");
        q1.setCorrectAnswerIds(List.of("ans-1-true"));

        CourseCorrectAnswerDto q2 = new CourseCorrectAnswerDto();
        q2.setQuestionId("q2");
        q2.setCorrectAnswerIds(List.of("ans-2-true"));

        when(courseServiceClient.getCorrectAnswers("quiz-1")).thenReturn(ApiResponse.success(List.of(q1, q2)));
        when(lessonProgressRepository.findByEnrollmentIdAndLessonId("enr-quiz-1", "quiz-1")).thenReturn(Optional.empty());
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        QuizSubmitRequest request = new QuizSubmitRequest();
        QuizAnswerRequest a1 = new QuizAnswerRequest();
        a1.setQuestionId("q1");
        a1.setAnswerId("ans-1-true");

        QuizAnswerRequest a2 = new QuizAnswerRequest();
        a2.setQuestionId("q2");
        a2.setAnswerId("ans-2-true");
        request.setAnswers(List.of(a1, a2));

        QuizResultResponse response = quizAttemptService.submitQuizAttempt("enr-quiz-1", "quiz-1", request, "learner-1");

        assertNotNull(response);
        assertEquals(100, response.getScore());
        assertTrue(response.getIsPassed());

        verify(quizAttemptRepository, times(1)).save(any(QuizAttempt.class));
        verify(lessonProgressRepository, times(1)).save(argThat(LessonProgress::getIsCompleted));
        verify(progressService, times(1)).recalculateCompletedRate(enrollment);
    }

    @Test
    @DisplayName("Nộp bài Quiz không đạt điểm (50% < 80%) -> Lưu attempt nhưng KHÔNG đánh dấu LessonProgress hoàn thành")
    void submitQuiz_failed_doesNotMarkProgressCompleted() {
        when(enrollmentRepository.findById("enr-quiz-1")).thenReturn(Optional.of(enrollment));

        CourseCorrectAnswerDto q1 = new CourseCorrectAnswerDto();
        q1.setQuestionId("q1");
        q1.setCorrectAnswerIds(List.of("ans-1-true"));

        CourseCorrectAnswerDto q2 = new CourseCorrectAnswerDto();
        q2.setQuestionId("q2");
        q2.setCorrectAnswerIds(List.of("ans-2-true"));

        when(courseServiceClient.getCorrectAnswers("quiz-1")).thenReturn(ApiResponse.success(List.of(q1, q2)));

        QuizSubmitRequest request = new QuizSubmitRequest();
        QuizAnswerRequest a1 = new QuizAnswerRequest();
        a1.setQuestionId("q1");
        a1.setAnswerId("ans-1-true");

        QuizAnswerRequest a2 = new QuizAnswerRequest();
        a2.setQuestionId("q2");
        a2.setAnswerId("ans-2-wrong"); // sai câu 2
        request.setAnswers(List.of(a1, a2));

        QuizResultResponse response = quizAttemptService.submitQuizAttempt("enr-quiz-1", "quiz-1", request, "learner-1");

        assertNotNull(response);
        assertEquals(50, response.getScore());
        assertFalse(response.getIsPassed());

        verify(quizAttemptRepository, times(1)).save(any(QuizAttempt.class));
        verify(lessonProgressRepository, never()).save(any(LessonProgress.class));
        verify(progressService, never()).recalculateCompletedRate(any());
    }
}
