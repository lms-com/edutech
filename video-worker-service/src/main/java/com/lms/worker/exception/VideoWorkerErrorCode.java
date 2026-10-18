package com.lms.worker.exception;

import com.lms.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum VideoWorkerErrorCode implements ErrorCode {

    FILE_NOT_FOUND(5001, "File not found from db", HttpStatus.NOT_FOUND),
    FAILED_PROCESSING_FILE(5002, "Failed processing file", HttpStatus.INTERNAL_SERVER_ERROR),
    ;
    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
