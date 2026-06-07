package com.lms.notification.exception;

import com.lms.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum EnrollmentErrorCode implements ErrorCode {
    ENROLLMENT_NOT_FOUND(4001, "Enrollment record not found", HttpStatus.NOT_FOUND),
    ALREADY_ENROLLED(4002, "Learner is already enrolled in this course", HttpStatus.BAD_REQUEST),
    INVALID_STATUS(4003, "Invalid enrollment status transition", HttpStatus.BAD_REQUEST),
    PROGRESS_NOT_FOUND(4004, "Lesson progress record not found", HttpStatus.NOT_FOUND),
    REVIEW_ALREADY_EXISTS(4005, "Learner has already reviewed this course", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED_ACCESS(4006, "You do not have access to this enrollment data", HttpStatus.FORBIDDEN);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    EnrollmentErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

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
        return httpStatus;
    }
}
