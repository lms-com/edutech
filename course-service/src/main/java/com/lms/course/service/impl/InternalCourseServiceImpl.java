package com.lms.course.service.impl;

import com.lms.common.exception.AppException;
import com.lms.course.dto.response.*;
import com.lms.course.entity.Answer;
import com.lms.course.entity.Course;
import com.lms.course.entity.Lesson;
import com.lms.course.entity.Question;
import com.lms.course.exception.CourseErrorCode;
import com.lms.course.repository.*;
import com.lms.course.service.InternalCourseService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InternalCourseServiceImpl implements InternalCourseService {

    CourseRepository courseRepository;
    SectionRepository sectionRepository;
    LessonRepository lessonRepository;
    QuestionRepository questionRepository;
    AnswerRepository answerRepository;

    // ======================== API 35 ========================
    /**
     * Cấp thông tin giá khóa học cho Order Service.
     * Order Service gọi API này khi người dùng bấm "Mua khóa học" để lấy giá chính xác.
     */
    @Override
    @Transactional(readOnly = true)
    public CoursePriceResponse getCoursePriceInfo(String courseId) {
        Course course = courseRepository.findByIdAndNotDeleted(courseId)
                .orElseThrow(() -> new AppException(CourseErrorCode.COURSE_NOT_FOUND));

        return CoursePriceResponse.builder()
                .courseId(course.getId())
                .title(course.getTitle())
                .basePrice(course.getBasePrice())
                .currencyCode(course.getCurrencyCode())
                .overrideCommissionRate(course.getOverrideCommissionRate())
                .instructorId(course.getInstructorId())
                .status(course.getStatus())
                .build();
    }

    // ======================== API 36 ========================
    /**
     * Cấp thông tin cơ bản nhiều khóa học cùng lúc (Batch).
     * Enrollment Service / Dashboard gọi API này khi cần hiển thị danh sách "Khóa học đã đăng ký".
     */
    @Override
    @Transactional(readOnly = true)
    public List<CourseBatchResponse> getCourseBatch(List<String> courseIds) {
        List<Course> courses = courseRepository.findAllByIdInAndNotDeleted(courseIds);

        return courses.stream().map(course -> CourseBatchResponse.builder()
                .courseId(course.getId())
                .title(course.getTitle())
                .slug(course.getSlug())
                .thumbnailUrl(course.getThumbnailUrl())
                .instructorId(course.getInstructorId())
                .level(course.getLevel())
                .status(course.getStatus())
                .build()
        ).collect(Collectors.toList());
    }

    // ======================== API 37 ========================
    /**
     * Đếm tổng bài học của một khóa học.
     * Enrollment Service gọi API này để tính phần trăm tiến độ: (bài đã hoàn thành / tổng bài) * 100.
     */
    @Override
    @Transactional(readOnly = true)
    public LessonCountResponse getLessonCount(String courseId) {
        // 1. Kiểm tra khóa học tồn tại
        courseRepository.findByIdAndNotDeleted(courseId)
                .orElseThrow(() -> new AppException(CourseErrorCode.COURSE_NOT_FOUND));

        // 2. Lấy danh sách section IDs thuộc khóa học này
        List<String> sectionIds = sectionRepository
                .findByCourseIdAndDeletedFalseOrderByOrderIndexAsc(courseId)
                .stream().map(s -> s.getId()).collect(Collectors.toList());

        // 3. Đếm tổng lesson thuộc các sections đó
        long totalLessons = sectionIds.isEmpty() ? 0 :
                lessonRepository.countBySectionIdsAndNotDeleted(sectionIds);

        return LessonCountResponse.builder()
                .courseId(courseId)
                .totalLessons(totalLessons)
                .build();
    }

    // ======================== API 38 ========================
    /**
     * Xác minh tính hợp lệ của bài học.
     * Enrollment Service gọi trước khi ghi nhận "đã hoàn thành bài học" để đảm bảo lesson thực sự tồn tại.
     */
    @Override
    @Transactional(readOnly = true)
    public LessonValidationResponse validateLesson(String lessonId) {
        return lessonRepository.findByIdAndDeletedFalse(lessonId)
                .map(lesson -> LessonValidationResponse.builder()
                        .lessonId(lesson.getId())
                        .courseId(lesson.getSection().getCourse().getId())
                        .type(lesson.getType())
                        .valid(true)
                        .build())
                .orElse(LessonValidationResponse.builder()
                        .lessonId(lessonId)
                        .valid(false)
                        .build());
    }

    // ======================== API 39 ========================
    /**
     * Cấp danh sách đáp án đúng cho Enrollment Service chấm điểm tự động.
     * Khi học viên nộp bài Quiz, Enrollment Service so khớp answer IDs với list này.
     */
    @Override
    @Transactional(readOnly = true)
    public List<CorrectAnswerResponse> getCorrectAnswers(String lessonId) {
        // 1. Lấy tất cả câu hỏi thuộc quiz này
        List<Question> questions = questionRepository.findByQuizIdAndNotDeletedOrderByOrderIndexAsc(lessonId);

        if (questions.isEmpty()) {
            throw new AppException(CourseErrorCode.LESSON_NOT_FOUND);
        }

        // 2. Map từng câu hỏi sang response kèm danh sách ID đáp án đúng
        return questions.stream().map(question -> {
            List<String> correctIds = answerRepository
                    .findByQuestionIdAndDeletedFalseAndCorrectTrue(question.getId())
                    .stream().map(Answer::getId).collect(Collectors.toList());

            return CorrectAnswerResponse.builder()
                    .questionId(question.getId())
                    .questionText(question.getQuestionText())
                    .correctAnswerIds(correctIds)
                    .build();
        }).collect(Collectors.toList());
    }

    // ======================== API 40 ========================
    /**
     * Cấp thông tin cơ bản + giá của nhiều khóa học cùng lúc.
     * Finance Service gọi API này khi nhận event order.completed để lấy basePrice + instructorId
     * phục vụ tính toán Revenue Split (chia hoa hồng).
     */
    @Override
    @Transactional(readOnly = true)
    public List<CourseBulkResponse> getCourseBulk(List<String> courseIds) {
        List<Course> courses = courseRepository.findAllByIdInAndNotDeleted(courseIds);

        return courses.stream().map(course -> CourseBulkResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .basePrice(course.getBasePrice())
                .currencyCode(course.getCurrencyCode())
                .instructorId(course.getInstructorId())
                .build()
        ).collect(Collectors.toList());
    }
}
