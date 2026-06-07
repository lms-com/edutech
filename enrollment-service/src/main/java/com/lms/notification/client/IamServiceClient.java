package com.lms.notification.client;

import com.lms.common.dto.response.ApiResponse;
import com.lms.notification.config.FeignConfig;
import com.lms.notification.dto.response.LearnerInfoResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(
    name = "iam-service",
    configuration = FeignConfig.class
)
public interface IamServiceClient {

    @GetMapping("/api/internal/v1/users/batch")
    @CircuitBreaker(name = "iam-service", fallbackMethod = "getUsersFallback")
    ApiResponse<List<LearnerInfoResponse>> getUsersByIds(
        @RequestParam("userIds") List<String> userIds
    );

    // Fallback khi IAM Service không phản hồi
    default ApiResponse<List<LearnerInfoResponse>> getUsersFallback(
            List<String> userIds, Exception e) {
        // Trả về list placeholder — không crash hệ thống
        List<LearnerInfoResponse> placeholders = userIds.stream()
            .map(id -> LearnerInfoResponse.builder()
                .id(id)
                .fullName("Học viên")
                .avatarUrl(null)
                .build())
            .toList();
        return ApiResponse.<List<LearnerInfoResponse>>builder()
            .code(200)
            .message("Fallback success")
            .data(placeholders)
            .build();
    }
}
