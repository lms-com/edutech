package com.lms.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
public enum CommonErrorCode implements ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized exception", HttpStatus.INTERNAL_SERVER_ERROR)

    // 1xxx: System error code:

    // 2xxx: IAM service error code:

    // 3xxx:
    ;
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
