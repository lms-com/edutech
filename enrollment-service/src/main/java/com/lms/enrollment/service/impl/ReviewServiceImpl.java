package com.lms.enrollment.service.impl;

import com.lms.common.dto.response.ApiResponse;
import com.lms.common.exception.AppException;
import com.lms.enrollment.client.IamServiceClient;
import com.lms.enrollment.dto.request.ReviewRequest;
import com.lms.enrollment.dto.response.LearnerInfoResponse;
import com.lms.enrollment.dto.response.RatingSummaryResponse;
import com.lms.enrollment.dto.response.ReviewResponse;
import com.lms.enrollment.entity.Enrollment;
import com.lms.enrollment.entity.Review;
import com.lms.enrollment.enums.EnrollmentStatus;
import com.lms.enrollment.exception.EnrollmentErrorCode;
import com.lms.enrollment.repository.EnrollmentRepository;
import com.lms.enrollment.repository.ReviewRepository;
import com.lms.enrollment.service.ReviewService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReviewServiceImpl implements ReviewService {

    static final String SUMMARY_CACHE_PREFIX = "enrollment:reviews:summary:";

    EnrollmentRepository enrollmentRepository;
    ReviewRepository reviewRepository;
    IamServiceClient iamServiceClient;
    RedisTemplate<String, Object> redisTemplate;

    /**
     * Tạo đánh giá (Review) mới cho khóa học.
     * Xác thực học viên đã ghi danh vào khóa học này và có trạng thái ACTIVE.
     * Đảm bảo ràng buộc nghiệp vụ: Mỗi lượt ghi danh chỉ được tạo tối đa 1 đánh giá (Review).
     */
    @Override
    @Transactional
    public ReviewResponse createReview(String courseId, ReviewRequest request, String userId) {
        Enrollment enrollment = enrollmentRepository.findByLearnerIdAndCourseId(userId, courseId)
                .orElseThrow(() -> new AppException(EnrollmentErrorCode.UNAUTHORIZED_ACCESS));

        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
            throw new AppException(EnrollmentErrorCode.UNAUTHORIZED_ACCESS);
        }

        // Kiểm tra xem đã tồn tại đánh giá nào (bao gồm cả đã xóa mềm) chưa
        java.util.Optional<Review> existingReviewOpt = reviewRepository.findByEnrollmentIdIncludingDeleted(enrollment.getId());

        Review review;
        if (existingReviewOpt.isPresent()) {
            Review existingReview = existingReviewOpt.get();
            if (!existingReview.getIsDeleted()) {
                // Đánh giá đang hoạt động -> báo lỗi
                throw new AppException(EnrollmentErrorCode.REVIEW_ALREADY_EXISTS);
            }
            // Nếu đã xóa mềm trước đó -> Khôi phục (Undelete) và cập nhật thông tin mới
            existingReview.setIsDeleted(false);
            existingReview.setStar(request.getStar());
            existingReview.setComment(request.getComment());
            review = reviewRepository.save(existingReview);
            log.info("Khôi phục đánh giá đã xóa mềm thành công cho enrollment ID: {}", enrollment.getId());
        } else {
            // Tạo mới hoàn toàn nếu chưa từng tồn tại
            review = Review.builder()
                    .id(UUID.randomUUID().toString())
                    .enrollment(enrollment)
                    .courseId(courseId)
                    .star(request.getStar())
                    .comment(request.getComment())
                    .build();
            review = reviewRepository.save(review);
        }

        evictRatingSummaryCache(courseId);
        return mapToResponse(review, fetchLearners(List.of(userId)).get(userId));
    }

    /**
     * Lấy danh sách đánh giá của khóa học hỗ trợ phân trang.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getCourseReviews(String courseId, Pageable pageable) {
        Page<Review> reviews = reviewRepository.findAllByCourseId(courseId, pageable);

        Map<String, LearnerInfoResponse> learners = fetchLearners(
                reviews.getContent().stream()
                        .map(review -> review.getEnrollment().getLearnerId())
                        .toList());

        return reviews.map(review ->
                mapToResponse(review, learners.get(review.getEnrollment().getLearnerId())));
    }

    /**
     * Tổng hợp điểm đánh giá trung bình và phân bố số sao (1-5 sao) cho khóa học.
     * Có cache Redis để tối ưu hiệu năng khi nhiều người xem chi tiết khóa học.
     */
    @Override
    @Transactional(readOnly = true)
    public RatingSummaryResponse getCourseRatingSummary(String courseId) {
        String cacheKey = SUMMARY_CACHE_PREFIX + courseId;
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof RatingSummaryResponse summary) {
                return summary;
            }
        } catch (Exception e) {
            log.debug("Bỏ qua đọc cache rating summary cho khóa học {}: {}", courseId, e.getMessage());
        }

        Map<Integer, Long> distribution = new LinkedHashMap<>();
        for (int star = 5; star >= 1; star--) {
            distribution.put(star, 0L);
        }

        List<Object[]> rows = reviewRepository.countReviewsByStar(courseId);
        long totalReviews = 0L;
        long weightedSum = 0L;

        if (rows != null) {
            for (Object[] row : rows) {
                if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                    int star = ((Number) row[0]).intValue();
                    long count = ((Number) row[1]).longValue();
                    if (star >= 1 && star <= 5) {
                        distribution.put(star, count);
                        totalReviews += count;
                        weightedSum += (long) star * count;
                    }
                }
            }
        }

        double averageRating = totalReviews > 0
                ? Math.round((weightedSum * 10.0) / totalReviews) / 10.0
                : 0.0;

        RatingSummaryResponse summary = RatingSummaryResponse.builder()
                .courseId(courseId)
                .averageRating(averageRating)
                .totalReviews(totalReviews)
                .starDistribution(distribution)
                .build();

        try {
            redisTemplate.opsForValue().set(cacheKey, summary, Duration.ofHours(1));
        } catch (Exception e) {
            log.debug("Bỏ qua lưu cache rating summary cho khóa học {}: {}", courseId, e.getMessage());
        }

        return summary;
    }

    /**
     * Học viên tự thực hiện xóa đánh giá của mình (Xóa mềm - Soft Delete).
     */
    @Override
    @Transactional
    public void deleteReview(String reviewId, String userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(EnrollmentErrorCode.PROGRESS_NOT_FOUND));

        if (!review.getEnrollment().getLearnerId().equals(userId)) {
            throw new AppException(EnrollmentErrorCode.UNAUTHORIZED_ACCESS);
        }

        review.setIsDeleted(true);
        reviewRepository.save(review);
        evictRatingSummaryCache(review.getCourseId());
    }

    /**
     * Cập nhật nội dung đánh giá (Số sao và bình luận).
     */
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
        evictRatingSummaryCache(review.getCourseId());

        return mapToResponse(review, fetchLearners(List.of(userId)).get(userId));
    }

    /**
     * Quản trị viên (Admin) thực hiện xóa đánh giá (Xóa mềm - Soft Delete).
     */
    @Override
    @Transactional
    public void adminDeleteReview(String reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(EnrollmentErrorCode.PROGRESS_NOT_FOUND));

        review.setIsDeleted(true);
        reviewRepository.save(review);
        evictRatingSummaryCache(review.getCourseId());
    }

    private void evictRatingSummaryCache(String courseId) {
        try {
            if (courseId != null) {
                redisTemplate.delete(SUMMARY_CACHE_PREFIX + courseId);
            }
        } catch (Exception e) {
            log.debug("Không xóa được cache rating summary cho khóa học {}: {}", courseId, e.getMessage());
        }
    }

    /**
     * Chuyển đổi thực thể Review sang DTO ReviewResponse.
     * `learner` có thể null khi iam-service không trả về thông tin.
     */
    private ReviewResponse mapToResponse(Review review, LearnerInfoResponse learner) {
        return ReviewResponse.builder()
                .id(review.getId())
                .enrollmentId(review.getEnrollment().getId())
                .courseId(review.getCourseId())
                .learnerId(review.getEnrollment().getLearnerId())
                .learnerName(learner != null ? learner.getFullName() : null)
                .learnerAvatar(learner != null ? learner.getAvatarUrl() : null)
                .star(review.getStar())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }

    /**
     * Lấy thông tin học viên theo ID bằng một lần gọi batch.
     * IamServiceClient đã có fallback trả nhãn trung tính khi iam-service không phản hồi.
     */
    private Map<String, LearnerInfoResponse> fetchLearners(List<String> learnerIds) {
        List<String> distinctIds = learnerIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (distinctIds.isEmpty()) {
            return Map.of();
        }
        try {
            ApiResponse<List<LearnerInfoResponse>> response = iamServiceClient.getUsersByIds(distinctIds);
            if (response == null || response.getData() == null) {
                return Map.of();
            }
            return response.getData().stream()
                    .filter(item -> item.getId() != null)
                    .collect(Collectors.toMap(LearnerInfoResponse::getId, item -> item, (first, second) -> first));
        } catch (Exception e) {
            log.warn("Không lấy được thông tin học viên từ iam-service: {}", e.getMessage());
            return Map.of();
        }
    }
}
