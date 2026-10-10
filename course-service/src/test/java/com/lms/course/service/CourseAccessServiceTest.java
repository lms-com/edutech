package com.lms.course.service;

import com.lms.common.dto.response.ApiResponse;
import com.lms.common.exception.AppException;
import com.lms.course.client.EnrollmentAccessClient;
import com.lms.course.dto.response.EnrollmentValidationResponse;
import com.lms.course.entity.Course;
import com.lms.course.entity.Section;
import com.lms.course.entity.VideoLesson;
import com.lms.course.exception.CourseErrorCode;
import com.lms.course.repository.CourseRepository;
import com.lms.course.repository.LessonRepository;
import com.lms.course.repository.QuestionRepository;
import com.lms.course.repository.SectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseAccessServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private EnrollmentAccessClient enrollmentAccessClient;

    @InjectMocks
    private CourseAccessService courseAccessService;

    private Course course;
    private Section section;
    private VideoLesson regularLesson;
    private VideoLesson freePreviewLesson;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(courseAccessService, "internalKey", "test-internal-key");

        course = new Course();
        course.setId("course-123");
        course.setInstructorId("inst-owner");

        section = new Section();
        section.setId("section-123");
        section.setCourse(course);

        regularLesson = new VideoLesson();
        regularLesson.setId("lesson-regular");
        regularLesson.setSection(section);
        regularLesson.setFreePreview(false);
        regularLesson.setVideoUrl("https://storage/video1.mp4");

        freePreviewLesson = new VideoLesson();
        freePreviewLesson.setId("lesson-free");
        freePreviewLesson.setSection(section);
        freePreviewLesson.setFreePreview(true);
        freePreviewLesson.setVideoUrl("https://storage/preview.mp4");
    }

    @Test
    @DisplayName("Giảng viên sở hữu khóa học có quyền quản lý khóa")
    void instructorOwner_canManageCourse() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "inst-owner", null, List.of(new SimpleGrantedAuthority("COURSE_UPDATE")));

        when(courseRepository.findByIdAndNotDeleted("course-123")).thenReturn(Optional.of(course));

        boolean canManage = courseAccessService.canManageCourse("course-123", auth);
        assertTrue(canManage);
        assertDoesNotThrow(() -> courseAccessService.requireManageCourse("course-123", auth));
    }

    @Test
    @DisplayName("Giảng viên khác không có quyền quản lý khóa của người khác -> Ném 403")
    void otherInstructor_cannotManageCourse() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "inst-intruder", null, List.of(new SimpleGrantedAuthority("COURSE_UPDATE")));

        when(courseRepository.findByIdAndNotDeleted("course-123")).thenReturn(Optional.of(course));

        boolean canManage = courseAccessService.canManageCourse("course-123", auth);
        assertFalse(canManage);

        AppException ex = assertThrows(AppException.class,
                () -> courseAccessService.requireManageCourse("course-123", auth));
        assertEquals(CourseErrorCode.COURSE_UNAUTHORIZED_ACCESS, ex.getErrorCode());
    }

    @Test
    @DisplayName("Admin có toàn quyền quản lý mọi khóa học")
    void admin_canManageAnyCourse() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "admin-user", null, List.of(new SimpleGrantedAuthority("ADMIN")));

        boolean canManage = courseAccessService.canManageCourse("course-123", auth);
        assertTrue(canManage);
        assertDoesNotThrow(() -> courseAccessService.requireManageCourse("course-123", auth));
    }

    @Test
    @DisplayName("Bài học Free Preview cho phép học viên học thử không cần ghi danh")
    void freePreviewLesson_allowsAccessWithoutEnrollment() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "learner-1", null, List.of(new SimpleGrantedAuthority("COURSE_LEARN")));

        when(lessonRepository.findByIdAndDeletedFalse("lesson-free"))
                .thenReturn(Optional.of(freePreviewLesson));

        assertDoesNotThrow(() -> courseAccessService.requireLessonAccess("lesson-free", auth));
    }

    @Test
    @DisplayName("Bài học thông thường bị chặn nếu học viên chưa có enrollment ACTIVE -> Ném 403")
    void regularLesson_blockedWhenNotEnrolled() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "learner-1", null, List.of(new SimpleGrantedAuthority("COURSE_LEARN")));

        when(lessonRepository.findByIdAndDeletedFalse("lesson-regular"))
                .thenReturn(Optional.of(regularLesson));

        EnrollmentValidationResponse noAccess = EnrollmentValidationResponse.builder()
                .hasAccess(false)
                .enrollmentStatus("NOT_FOUND")
                .build();
        when(enrollmentAccessClient.validateAccess(eq("learner-1"), eq("course-123"), anyString()))
                .thenReturn(ApiResponse.success(noAccess));

        AppException ex = assertThrows(AppException.class,
                () -> courseAccessService.requireLessonAccess("lesson-regular", auth));
        assertEquals(CourseErrorCode.COURSE_UNAUTHORIZED_ACCESS, ex.getErrorCode());
    }

    @Test
    @DisplayName("Bài học thông thường được mở khi học viên có enrollment ACTIVE")
    void regularLesson_allowedWhenEnrollmentActive() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "learner-active", null, List.of(new SimpleGrantedAuthority("COURSE_LEARN")));

        when(lessonRepository.findByIdAndDeletedFalse("lesson-regular"))
                .thenReturn(Optional.of(regularLesson));

        EnrollmentValidationResponse activeAccess = EnrollmentValidationResponse.builder()
                .hasAccess(true)
                .enrollmentStatus("ACTIVE")
                .build();
        when(enrollmentAccessClient.validateAccess(eq("learner-active"), eq("course-123"), anyString()))
                .thenReturn(ApiResponse.success(activeAccess));

        assertDoesNotThrow(() -> courseAccessService.requireLessonAccess("lesson-regular", auth));
    }
}
