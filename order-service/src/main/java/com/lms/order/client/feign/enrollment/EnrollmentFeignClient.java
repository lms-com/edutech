package com.lms.order.client.feign.enrollment;

import com.lms.common.dto.response.ApiResponse;
import com.lms.order.client.feign.enrollment.dto.EnrollmentValidationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "enrollment-service", path = "/api/internal/v1/enrollments")
public interface EnrollmentFeignClient {

    @GetMapping("/validation")
    ApiResponse<EnrollmentValidationResponse> validateAccess(
            @RequestParam("learnerId") String learnerId,
            @RequestParam("courseId") String courseId);

    @PostMapping("/revoke")
    ApiResponse<Void> revokeAccess(
            @RequestParam("learnerId") String learnerId,
            @RequestParam("courseId") String courseId);
}
