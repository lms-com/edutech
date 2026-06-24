package com.lms.enrollment.controller;

import com.lms.common.dto.response.ApiResponse;
import com.lms.enrollment.dto.request.ReviewRequest;
import com.lms.enrollment.dto.response.ReviewResponse;
import com.lms.enrollment.service.ReviewService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReviewController {

    ReviewService reviewService;

    @PostMapping("/courses/{courseId}")
    public ApiResponse<ReviewResponse> createReview(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String courseId,
            @Valid @RequestBody ReviewRequest request) {
        return ApiResponse.success(reviewService.createReview(courseId, request, userId));
    }

    @GetMapping("/courses/{courseId}")
    public ApiResponse<Page<ReviewResponse>> getCourseReviews(
            @PathVariable String courseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(reviewService.getCourseReviews(courseId, PageRequest.of(page, size)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteReview(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String id) {
        reviewService.deleteReview(id, userId);
        return ApiResponse.success(null);
    }

    @PutMapping("/{id}")
    public ApiResponse<ReviewResponse> updateReview(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String id,
            @Valid @RequestBody ReviewRequest request) {
        return ApiResponse.success(reviewService.updateReview(id, request, userId));
    }

    @DeleteMapping("/admin/{id}")
    public ApiResponse<Void> adminDeleteReview(@PathVariable String id) {
        reviewService.adminDeleteReview(id);
        return ApiResponse.success(null);
    }
}
