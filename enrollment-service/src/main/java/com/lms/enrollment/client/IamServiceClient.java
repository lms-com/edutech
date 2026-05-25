package com.lms.enrollment.client;

import com.lms.common.dto.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "iam-service")
public interface IamServiceClient {

    @GetMapping("/api/internal/v1/users/{userId}")
    ApiResponse<Object> getUserDetails(@PathVariable("userId") String userId);
}
