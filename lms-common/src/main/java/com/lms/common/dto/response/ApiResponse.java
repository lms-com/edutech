package com.lms.common.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Standard reponse structure of system")
public class ApiResponse <T>{
    @Schema(description = "Internal server code", example = "1000")
    private int code;
    @Schema(description = "Detail message", example = "Success")
    private String message;
    @Schema(description = "Returned data")
    private T data;
    @Schema(description = "Error tracing code for system lookup", example = "a1b2c3d4e5f6g7h8")
    private String traceId;

    public static <T> ApiResponse<T> success (T data){
        return ApiResponse.<T>builder()
                .code(200)
                .message("OK")
                .data(data)
                .build();
    }

    public static ApiResponse<?> error (int statusCode, String message){
        return ApiResponse.builder()
                .code(statusCode)
                .message(message)
                .build();
    }
}
