package com.lms.course.service;

import com.lms.common.dto.response.ApiResponse;
import com.lms.common.exception.AppException;
import com.lms.course.client.EnrollmentAccessClient;
import com.lms.course.dto.response.EnrollmentValidationResponse;
import com.lms.course.entity.Course;
import com.lms.course.entity.Lesson;
import com.lms.course.entity.Section;
import com.lms.course.exception.CourseErrorCode;
import com.lms.course.repository.CourseRepository;
import com.lms.course.repository.LessonRepository;
import com.lms.course.repository.QuestionRepository;
import com.lms.course.repository.SectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CourseAccessService {

    private final CourseRepository courseRepository;
    private final SectionRepository sectionRepository;
    private final LessonRepository lessonRepository;
    private final QuestionRepository questionRepository;
    private final EnrollmentAccessClient enrollmentAccessClient;

    @Value("${application.security.internal-key}")
    private String internalKey;

    public boolean canManageCourse(String courseId, Authentication authentication) {
        if (has(authentication, "COURSE_APPROVE") || has(authentication, "ADMIN")) return true;
        if (authentication == null || authentication.getName() == null) return false;
        return courseRepository.findByIdAndNotDeleted(courseId)
                .map(course -> course.getInstructorId().equals(authentication.getName())
                        && has(authentication, "COURSE_UPDATE"))
                .orElse(false);
    }

    public void requireManageCourse(String courseId, Authentication authentication) {
        if (!canManageCourse(courseId, authentication)) {
            throw new AppException(CourseErrorCode.COURSE_UNAUTHORIZED_ACCESS);
        }
    }

    public boolean hasActiveEnrollment(String courseId, String userId) {
        ApiResponse<EnrollmentValidationResponse> response = enrollmentAccessClient.validateAccess(
                userId, courseId, internalKey);
        return response != null && response.getData() != null
                && Boolean.TRUE.equals(response.getData().getHasAccess());
    }

    public void requireLessonAccess(String lessonId, Authentication authentication) {
        if (canManageLesson(lessonId, authentication)) return;
        Lesson lesson = lessonRepository.findByIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new AppException(CourseErrorCode.LESSON_NOT_FOUND));
        String courseId = lesson.getSection().getCourse().getId();
        if (authentication == null || !has(authentication, "COURSE_LEARN")
                || !hasActiveEnrollment(courseId, authentication.getName())) {
            throw new AppException(CourseErrorCode.COURSE_UNAUTHORIZED_ACCESS);
        }
    }

    public boolean canManageLesson(String lessonId, Authentication authentication) {
        Lesson lesson = lessonRepository.findByIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new AppException(CourseErrorCode.LESSON_NOT_FOUND));
        return canManageCourse(lesson.getSection().getCourse().getId(), authentication);
    }

    public void requireManageSection(String sectionId, Authentication authentication) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new AppException(CourseErrorCode.SECTION_NOT_FOUND));
        requireManageCourse(section.getCourse().getId(), authentication);
    }

    public void requireManageLesson(String lessonId, Authentication authentication) {
        Lesson lesson = lessonRepository.findByIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new AppException(CourseErrorCode.LESSON_NOT_FOUND));
        requireManageCourse(lesson.getSection().getCourse().getId(), authentication);
    }

    public void requireManageQuestion(String questionId, Authentication authentication) {
        var question = questionRepository.findById(questionId)
                .orElseThrow(() -> new AppException(CourseErrorCode.QUESTION_NOT_FOUND));
        requireManageCourse(question.getQuiz().getSection().getCourse().getId(), authentication);
    }

    public void requireManageCourseForSection(String courseId, Authentication authentication) {
        requireManageCourse(courseId, authentication);
    }

    public void requireManageCourseForLesson(String lessonId, Authentication authentication) {
        requireManageLesson(lessonId, authentication);
    }

    private boolean has(Authentication authentication, String authority) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(granted -> authority.equals(granted.getAuthority()));
    }
}
