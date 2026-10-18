package com.lms.enrollment.service;

import com.lms.common.dto.response.ApiResponse;
import com.lms.enrollment.client.CourseServiceClient;
import com.lms.enrollment.client.dto.LessonCountResponse;
import com.lms.enrollment.dto.event.CourseCompletedEvent;
import com.lms.enrollment.dto.request.ProgressUpdateRequest;
import com.lms.enrollment.dto.response.LessonProgressResponse;
import com.lms.enrollment.entity.Enrollment;
import com.lms.enrollment.entity.LessonProgress;
import com.lms.enrollment.messaging.EnrollmentPublisher;
import com.lms.enrollment.repository.EnrollmentRepository;
import com.lms.enrollment.repository.LessonProgressRepository;
import com.lms.enrollment.service.impl.ProgressServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProgressServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private LessonProgressRepository lessonProgressRepository;

    @Mock
    private CourseServiceClient courseServiceClient;

    @Mock
    private EnrollmentPublisher enrollmentPublisher;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private SetOperations<String, Object> setOperations;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @InjectMocks
    private ProgressServiceImpl progressService;

    private Enrollment enrollment;

    @BeforeEach
    void setUp() {
        enrollment = Enrollment.builder()
                .id("enr-100")
                .courseId("course-100")
                .learnerId("learner-1")
                .completedRate(0)
                .build();
    }

    @Test
    @DisplayName("Cập nhật hoàn thành bài học và tự động kích hoạt tính lại tiến độ khóa học")
    void updateLessonProgress_completed_success() {
        when(enrollmentRepository.findById("enr-100")).thenReturn(Optional.of(enrollment));
        when(lessonProgressRepository.findByEnrollmentIdAndLessonId("enr-100", "lesson-1"))
                .thenReturn(Optional.empty());
        when(lessonProgressRepository.save(any(LessonProgress.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        LessonCountResponse countResp = LessonCountResponse.builder()
                .courseId("course-100")
                .totalLessons(2L)
                .build();
        when(courseServiceClient.getLessonCount("course-100")).thenReturn(ApiResponse.success(countResp));
        when(lessonProgressRepository.countByEnrollmentIdAndIsCompletedTrue("enr-100")).thenReturn(1L);

        ProgressUpdateRequest request = new ProgressUpdateRequest();
        request.setIsCompleted(true);
        request.setLastWatchTimeSeconds(300);

        LessonProgressResponse response = progressService.updateLessonProgress("enr-100", "lesson-1", request, "learner-1");

        assertNotNull(response);
        assertTrue(response.getIsCompleted());
        assertEquals("lesson-1", response.getLessonId());
        assertEquals(50, enrollment.getCompletedRate()); // 1/2 = 50%
        verify(enrollmentRepository, times(1)).save(enrollment);
    }

    @Test
    @DisplayName("Khi hoàn thành 100% bài học -> Tự động bắn CourseCompletedEvent lên RabbitMQ")
    void recalculateCompletedRate_reaches100_publishesCourseCompletedEvent() {
        LessonCountResponse countResp = LessonCountResponse.builder()
                .courseId("course-100")
                .totalLessons(2L)
                .build();
        when(courseServiceClient.getLessonCount("course-100")).thenReturn(ApiResponse.success(countResp));
        when(lessonProgressRepository.countByEnrollmentIdAndIsCompletedTrue("enr-100")).thenReturn(2L);

        progressService.recalculateCompletedRate(enrollment);

        assertEquals(100, enrollment.getCompletedRate());
        verify(enrollmentRepository, times(1)).save(enrollment);
        verify(enrollmentPublisher, times(1)).publishCourseCompleted(any(CourseCompletedEvent.class));
    }
}
