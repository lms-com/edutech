package com.lms.notification.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.lms.common.dto.response.ApiResponse;
import com.lms.notification.dto.response.LearnerInfoResponse;

@FeignClient(name = "iam-service")
public interface IamServiceClient {
    @GetMapping("/api/internal/v1/users/batch")
    ApiResponse<List<LearnerInfoResponse>> getUserByIds(@RequestParam("userIds") List<String> userIds);
}
