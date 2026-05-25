package com.lms.enrollment.controller;

import com.lms.common.dto.response.ApiResponse;
import com.lms.enrollment.dto.response.EnrollmentResponse;
import com.lms.enrollment.service.EnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal/v1/enrollments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Internal Enrollment API", description = "Internal communication between microservices")
public class InternalEnrollmentController {

    EnrollmentService enrollmentService;

    @Operation(summary = "Check learner enrollment", description = "Verifies if learner is enrolled in course")
    @GetMapping("/check")
    public ApiResponse<EnrollmentResponse> checkEnrollment(
            @RequestParam String learnerId,
            @RequestParam String courseId) {
        return ApiResponse.success(enrollmentService.getEnrollment(learnerId, courseId));
    }
}
