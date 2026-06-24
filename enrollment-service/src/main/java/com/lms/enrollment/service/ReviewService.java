package com.lms.enrollment.service;

import com.lms.enrollment.dto.request.ReviewRequest;
import com.lms.enrollment.dto.response.ReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ReviewService {
    ReviewResponse createReview(String courseId, ReviewRequest request, String userId);
    Page<ReviewResponse> getCourseReviews(String courseId, Pageable pageable);
    void deleteReview(String reviewId, String userId);
    ReviewResponse updateReview(String reviewId, ReviewRequest request, String userId);
    void adminDeleteReview(String reviewId);
}
