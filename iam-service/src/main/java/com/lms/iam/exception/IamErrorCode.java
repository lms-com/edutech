package com.lms.iam.exception;

import com.lms.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum IamErrorCode implements ErrorCode {

    USER_NOT_EXISTED(2001, "User does not exist", HttpStatus.NOT_FOUND),
    PASSWORD_INCORRECT(2002, "Incorrect password", HttpStatus.BAD_REQUEST),
    EMAIL_ALREADY_EXISTS(2003, "Email already exists", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD_FORMAT(2004, "Invalid password format", HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED(2005, "Failed to authenticate", HttpStatus.UNAUTHORIZED),
    DEVICE_FINGERPRINT_REQUIRED(2006, "Device fingerprint required", HttpStatus.BAD_REQUEST)
    ;

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
