package com.lms.enrollment.controller.internal;

import com.lms.common.dto.response.ApiResponse;
import com.lms.enrollment.dto.response.EnrollmentValidationResponse;
import com.lms.enrollment.service.EnrollmentService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Hidden  // Ẩn khỏi Swagger public
@RestController
@RequestMapping("/api/internal/v1/enrollments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InternalEnrollmentController {

    EnrollmentService enrollmentService;

    @GetMapping("/validation")
    public ResponseEntity<ApiResponse<EnrollmentValidationResponse>> validateAccess(
            @RequestParam String learnerId,
            @RequestParam String courseId) {

        EnrollmentValidationResponse result =
            enrollmentService.validateAccess(learnerId, courseId);

        return ResponseEntity.ok(ApiResponse.<EnrollmentValidationResponse>builder()
            .code(200)
            .message("Success")
            .data(result)
            .build());
    }
}
