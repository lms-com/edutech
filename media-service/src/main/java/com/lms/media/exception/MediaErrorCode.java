package com.lms.media.exception;

import com.lms.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum MediaErrorCode implements ErrorCode {
    INITIALIZE_BUCKET_MINIO_FAILED(4001, "Initialize Minio Bucket Failed", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_NOT_FOUND(4002, "File Not Found", HttpStatus.NOT_FOUND),
    FILE_NOT_AVAILABLE(4003, "File Not Available", HttpStatus.SERVICE_UNAVAILABLE),
    UNAUTHORIZED(4004, "Unauthorized", HttpStatus.UNAUTHORIZED),
    UNACCEPTABLE(4005, "Unacceptable", HttpStatus.BAD_REQUEST),
    LACK_OF_ENCRYPTION_KEY(4006, "Lack of Encryption Key", HttpStatus.BAD_REQUEST),
    ;

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
