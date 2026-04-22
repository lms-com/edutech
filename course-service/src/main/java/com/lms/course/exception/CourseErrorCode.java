package com.lms.course.exception;

import com.lms.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
public enum CourseErrorCode implements ErrorCode {

    // 3xxx: Course service error code
    COURSE_SLUG_EXISTS(3001, "Slug khóa học đã tồn tại", HttpStatus.BAD_REQUEST),
    CATEGORY_NOT_FOUND(3002, "Không tìm thấy danh mục", HttpStatus.NOT_FOUND),
    COURSE_NOT_FOUND(3003, "Không tìm thấy khóa học", HttpStatus.NOT_FOUND);
    private final int code;
    private final String message;
    private final HttpStatus status;

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return status;
    }
}