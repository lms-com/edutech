package com.lms.order.exception;

import com.lms.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum OrderErrorCode implements ErrorCode {
    PROMOTION_NOT_FOUND (6001, "Promotion not found", HttpStatus.NOT_FOUND),
    PROMOTION_INACTIVE (6002, "Promotion inactive", HttpStatus.FORBIDDEN),
    PROMOTION_NOT_APPLICABLE (6003, "Promotion not applicable", HttpStatus.BAD_REQUEST),
    PROMOTION_LIMIT_USAGE_EXCEEDED (6004, "Promotion limit exceeded", HttpStatus.BAD_REQUEST),
    PROMOTION_NOT_STARTED_YET (6005, "Promotion not started yet", HttpStatus.BAD_REQUEST),
    PROMOTION_EXPIRED (6006, "Promotion has expired", HttpStatus.BAD_REQUEST),

    CURRENCY_NOT_SUPPORTED (6007, "Currency not supported", HttpStatus.BAD_REQUEST),
    ;
    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
