package com.lms.course.exception;

import com.lms.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
public enum CourseErrorCode implements ErrorCode {

    // 30xx: Category Errors
    CATEGORY_NOT_FOUND(3001, "Không tìm thấy danh mục", HttpStatus.NOT_FOUND),
    CATEGORY_SLUG_EXISTS(3002, "Slug danh mục đã tồn tại", HttpStatus.BAD_REQUEST),
    CATEGORY_HAS_CHILDREN(3003, "Không thể xóa danh mục đang chứa danh mục con", HttpStatus.BAD_REQUEST),
    CATEGORY_HAS_COURSES(3004, "Không thể xóa danh mục đang chứa khóa học", HttpStatus.BAD_REQUEST),
    CATEGORY_PARENT_NOT_FOUND(3005, "Không tìm thấy danh mục cha", HttpStatus.BAD_REQUEST),

    // 31xx: Course Errors
    COURSE_NOT_FOUND(3101, "Không tìm thấy khóa học", HttpStatus.NOT_FOUND),
    COURSE_SLUG_EXISTS(3102, "Slug khóa học đã tồn tại", HttpStatus.BAD_REQUEST),
    COURSE_INVALID_STATUS(3103, "Trạng thái khóa học không hợp lệ cho thao tác này", HttpStatus.BAD_REQUEST),
    COURSE_UNAUTHORIZED_ACCESS(3104, "Không có quyền thực hiện thao tác trên khóa học này", HttpStatus.FORBIDDEN),
    COURSE_NOT_PUBLISHED(3105, "Khóa học chưa được xuất bản", HttpStatus.BAD_REQUEST),
    COURSE_ALREADY_APPROVED(3106, "Khóa học đã được duyệt", HttpStatus.BAD_REQUEST),
    COURSE_ALREADY_REJECTED(3107, "Khóa học đã bị từ chối", HttpStatus.BAD_REQUEST),
    COURSE_MISSING_REQUIREMENTS(3108, "Khóa học chưa đủ điều kiện để xuất bản (cần có ít nhất 1 bài học, giá, thumbnail...)", HttpStatus.BAD_REQUEST),

    // 32xx: Section Errors
    SECTION_NOT_FOUND(3201, "Không tìm thấy chương học", HttpStatus.NOT_FOUND),
    SECTION_UNAUTHORIZED_ACCESS(3202, "Không có quyền thực hiện thao tác trên chương học này", HttpStatus.FORBIDDEN),

    // 33xx: Lesson Errors
    LESSON_NOT_FOUND(3301, "Không tìm thấy bài học", HttpStatus.NOT_FOUND),
    LESSON_UNAUTHORIZED_ACCESS(3302, "Không có quyền thực hiện thao tác trên bài học này", HttpStatus.FORBIDDEN),
    LESSON_INVALID_TYPE(3303, "Loại bài học không hợp lệ", HttpStatus.BAD_REQUEST),
    LESSON_CONTENT_MISSING(3304, "Nội dung bài học không được để trống", HttpStatus.BAD_REQUEST),

    // 34xx: Question Errors
    QUESTION_NOT_FOUND(3401, "Không tìm thấy câu hỏi", HttpStatus.NOT_FOUND),
    QUESTION_UNAUTHORIZED_ACCESS(3402, "Không có quyền thực hiện thao tác trên câu hỏi này", HttpStatus.FORBIDDEN),
    QUESTION_NO_CORRECT_ANSWER(3403, "Câu hỏi trắc nghiệm phải có ít nhất một đáp án đúng", HttpStatus.BAD_REQUEST),
    ANSWER_NOT_FOUND(3404, "Không tìm thấy đáp án", HttpStatus.NOT_FOUND),

    // 35xx: Media & Upload Errors
    UPLOAD_FAILED(3501, "Tải tệp lên thất bại", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_INVALID_TYPE(3502, "Định dạng tệp không hợp lệ", HttpStatus.BAD_REQUEST),
    FILE_TOO_LARGE(3503, "Kích thước tệp vượt quá giới hạn", HttpStatus.PAYLOAD_TOO_LARGE),

    // 39xx: General / Others
    UNAUTHORIZED_ACTION(3901, "Không có quyền thực hiện hành động này", HttpStatus.UNAUTHORIZED),
    INVALID_REORDER_DATA(3902, "Dữ liệu sắp xếp không hợp lệ", HttpStatus.BAD_REQUEST),
    INTERNAL_SERVER_ERROR(3999, "Lỗi hệ thống nội bộ Course Service", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus status;

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatus getHttpStatus() {
        return status;
    }
}
