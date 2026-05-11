package com.lms.common.exception;

import com.lms.common.dto.response.ApiResponse;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    private final Tracer tracer;

    @ExceptionHandler(value= AppException.class)
    public ResponseEntity<ApiResponse<?>> handlingAppException (AppException e){
        String traceId = tracer.currentSpan() != null ? tracer.currentSpan().context().traceId() : null;
        ApiResponse<?> response = ApiResponse.builder()
                .code(e.getErrorCode().getCode())
                .message(e.getErrorMessage())
                .traceId(traceId)
                .build();
        return ResponseEntity.status(e.getErrorCode().getHttpStatus()).body(response);
    }

    @ExceptionHandler(value= MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handlingValidation (MethodArgumentNotValidException e){
        String messgage = e.getBindingResult().getFieldError().getDefaultMessage();

        ApiResponse<?> response = ApiResponse.builder()
                .code(400)
                .message(messgage)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(value= BadCredentialsException.class)
    public ResponseEntity<ApiResponse<?>> handlingBadCredentialsException (BadCredentialsException e){
        String traceId = tracer.currentSpan() != null ? tracer.currentSpan().context().traceId() : null;
        ApiResponse<?> response = ApiResponse.builder()
                .code(2005)
                .message(e.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(value= DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<?>> handlingDataIntegrityViolationException(DataIntegrityViolationException e) {
        String traceId = tracer.currentSpan() != null ? tracer.currentSpan().context().traceId() : null;
        String message = "Lỗi toàn vẹn dữ liệu";

        // Trích xuất thông điệp lỗi cụ thể từ MySQL (ví dụ: Duplicate entry)
        if (e.getRootCause() != null) {
            message = e.getRootCause().getMessage();
        }

        ApiResponse<?> response = ApiResponse.builder()
                .code(400) // Có thể định nghĩa một mã lỗi cụ thể trong CommonErrorCode
                .message(message)
                .traceId(traceId)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(value=Exception.class)
    public ResponseEntity<ApiResponse<?>> handlingException (Exception e){
        e.printStackTrace();
        String traceId = tracer.currentSpan() != null ? tracer.currentSpan().context().traceId() : null;
        CommonErrorCode errorCode = CommonErrorCode.UNCATEGORIZED_EXCEPTION;
        
        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                // Gắn thêm message thực tế của lỗi thay vì chỉ "Uncategorized exception" để dễ debug
                .message(errorCode.getMessage() + ": " + e.getMessage()) 
                .traceId(traceId)
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
    }
}
