package com.lms.finance.exception;

import com.lms.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum FinanceErrorCode implements ErrorCode {
    INSUFFICTION_BALANCE (7001, "The balance is insufficient to complete the transaction", HttpStatus.BAD_REQUEST),
    INVALID_AMOUNT (7002, "Invalid amount", HttpStatus.BAD_REQUEST),
    UNSUPPORTED_PAYMENT_METHOD (7003, "Unsupported payment method", HttpStatus.BAD_REQUEST),
    INVALID_PAYMENT (7004, "Invalid payment", HttpStatus.BAD_REQUEST),
    INSTRUCTOR_BALANCE_NOT_EXISTS (7005, "Instructor balance not exists", HttpStatus.BAD_REQUEST),
    ;

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
