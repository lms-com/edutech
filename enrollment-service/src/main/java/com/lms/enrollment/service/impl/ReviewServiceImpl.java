package com.lms.enrollment.service.impl;

import com.lms.common.exception.AppException;
import com.lms.enrollment.dto.request.ReviewRequest;
import com.lms.enrollment.dto.response.ReviewResponse;
import com.lms.enrollment.entity.Enrollment;
import com.lms.enrollment.entity.Review;
import com.lms.enrollment.exception.EnrollmentErrorCode;
import com.lms.enrollment.repository.EnrollmentRepository;
import com.lms.enrollment.repository.ReviewRepository;
import com.lms.enrollment.service.ReviewService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReviewServiceImpl implements ReviewService {

    EnrollmentRepository enrollmentRepository;
    ReviewRepository reviewRepository;

    @Override
    @Transactional
    public ReviewResponse createReview(String courseId, ReviewRequest request, String userId) {
        Enrollment enrollment = enrollmentRepository.findByLearnerIdAndCourseId(userId, courseId)
                .orElseThrow(() -> new AppException(EnrollmentErrorCode.UNAUTHORIZED_ACCESS));

        if (reviewRepository.existsByEnrollmentId(enrollment.getId())) {
            throw new AppException(EnrollmentErrorCode.REVIEW_ALREADY_EXISTS);
        }

        Review review = Review.builder()
                .id(UUID.randomUUID().toString())
                .enrollment(enrollment)
                .courseId(courseId)
                .star(request.getStar())
                .comment(request.getComment())
                .build();

        review = reviewRepository.save(review);

        return mapToResponse(review);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getCourseReviews(String courseId, Pageable pageable) {
        return reviewRepository.findAllByCourseId(courseId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional
    public void deleteReview(String reviewId, String userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(EnrollmentErrorCode.PROGRESS_NOT_FOUND));

        if (!review.getEnrollment().getLearnerId().equals(userId)) {
            throw new AppException(EnrollmentErrorCode.UNAUTHORIZED_ACCESS);
        }

        reviewRepository.delete(review);
    }

    @Override
    @Transactional
    public ReviewResponse updateReview(String reviewId, ReviewRequest request, String userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(EnrollmentErrorCode.PROGRESS_NOT_FOUND));

        if (!review.getEnrollment().getLearnerId().equals(userId)) {
            throw new AppException(EnrollmentErrorCode.UNAUTHORIZED_ACCESS);
        }

        review.setStar(request.getStar());
        review.setComment(request.getComment());
        review = reviewRepository.save(review);

        return mapToResponse(review);
    }

    @Override
    @Transactional
    public void adminDeleteReview(String reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(EnrollmentErrorCode.PROGRESS_NOT_FOUND));

        reviewRepository.delete(review);
    }

    private ReviewResponse mapToResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .enrollmentId(review.getEnrollment().getId())
                .courseId(review.getCourseId())
                .learnerId(review.getEnrollment().getLearnerId())
                .star(review.getStar())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
