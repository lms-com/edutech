package com.lms.course.client;

import com.lms.common.dto.response.ApiResponse;
import com.lms.course.dto.response.EnrollmentValidationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "enrollment-service", path = "/api/internal/v1/enrollments")
public interface EnrollmentAccessClient {

    @GetMapping("/validation")
    ApiResponse<EnrollmentValidationResponse> validateAccess(
            @RequestParam("learnerId") String learnerId,
            @RequestParam("courseId") String courseId,
            @RequestHeader("X-Internal-Key") String internalKey);
}
