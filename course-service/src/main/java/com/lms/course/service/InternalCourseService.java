package com.lms.course.service;

import com.lms.course.dto.response.*;

import java.util.List;

/**
 * Service xử lý logic cho các Internal APIs (giao tiếp nội bộ giữa các microservice).
 */
public interface InternalCourseService {

    /** API 35: Lấy thông tin giá khóa học cho Order Service */
    CoursePriceResponse getCoursePriceInfo(String courseId);

    /** API 36: Lấy thông tin cơ bản nhiều khóa học cùng lúc */
    List<CourseBatchResponse> getCourseBatch(List<String> courseIds);

    /** API 37: Đếm tổng bài học của một khóa học */
    LessonCountResponse getLessonCount(String courseId);

    /** API 38: Xác minh bài học có hợp lệ không */
    LessonValidationResponse validateLesson(String lessonId);

    /** API 39: Lấy danh sách đáp án đúng cho chấm điểm tự động */
    List<CorrectAnswerResponse> getCorrectAnswers(String lessonId);

    /** API 40: Lấy thông tin cơ bản + giá của nhiều khóa học (Finance Service gọi để Revenue Split) */
    List<CourseBulkResponse> getCourseBulk(List<String> courseIds);
}
