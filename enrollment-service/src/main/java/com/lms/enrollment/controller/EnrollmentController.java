package com.lms.enrollment.controller;

import com.lms.common.dto.response.ApiResponse;
import com.lms.enrollment.dto.response.EnrollmentDetailResponse;
import com.lms.enrollment.dto.response.EnrollmentResponse;
import com.lms.enrollment.service.EnrollmentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/enrollments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EnrollmentController {

    EnrollmentService enrollmentService;

    @PostMapping("/courses/{courseId}")
    public ApiResponse<EnrollmentResponse> enrollLearner(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String courseId) {
        return ApiResponse.success(enrollmentService.enrollLearner(userId, courseId));
    }

    @GetMapping("/{id}")
    public ApiResponse<EnrollmentDetailResponse> getEnrollmentDetail(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String id) {
        return ApiResponse.success(enrollmentService.getEnrollmentDetail(id, userId));
    }

    @GetMapping("/my")
    public ApiResponse<Page<EnrollmentResponse>> getMyEnrollments(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(enrollmentService.getMyEnrollments(userId, PageRequest.of(page, size)));
    }

    @PutMapping("/{id}/revoke")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ApiResponse<Void> revokeEnrollment(@PathVariable String id) {
        enrollmentService.revokeEnrollment(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/courses/{courseId}")
    public ApiResponse<Page<EnrollmentResponse>> getCourseEnrollments(
            @PathVariable String courseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(enrollmentService.getCourseEnrollments(courseId, PageRequest.of(page, size)));
    }
}
