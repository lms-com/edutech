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
    DEVICE_FINGERPRINT_REQUIRED(2006, "Device fingerprint required", HttpStatus.BAD_REQUEST),
    ROLE_ALREADY_EXISTS(2007, "Role already exists", HttpStatus.BAD_REQUEST),
    ROLE_NOT_FOUND(2008, "Role not found", HttpStatus.NOT_FOUND),
    LEARNER_PROFILE_NOT_FOUND(2009, "Learner profile not found", HttpStatus.NOT_FOUND),
    INSTRUCTOR_PROFILE_NOT_FOUND(2010, "Instructor profile not found", HttpStatus.NOT_FOUND),
    DEVICE_IS_BLOCKED(2012, "Device is blocked for this account", HttpStatus.BAD_REQUEST),
    JWT_TOKEN_INVALID(2013, "JWT token invalid", HttpStatus.BAD_REQUEST),
    USER_LOCKED(2014, "User locked", HttpStatus.FORBIDDEN),
    USER_DISABLED(2015, "User disabled", HttpStatus.FORBIDDEN),
    ;

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
