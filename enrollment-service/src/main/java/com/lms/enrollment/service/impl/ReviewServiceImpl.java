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

import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReviewServiceImpl implements ReviewService {

    EnrollmentRepository enrollmentRepository;
    ReviewRepository reviewRepository;

    /**
     * Tạo đánh giá (Review) mới cho khóa học.
     * Xác thực học viên đã ghi danh vào khóa học này chưa (nếu chưa thì báo lỗi unauthorized).
     * Đảm bảo ràng buộc nghiệp vụ: Mỗi lượt ghi danh chỉ được tạo tối đa 1 đánh giá (Review).
     *
     * @param courseId ID khóa học cần đánh giá
     * @param request Nội dung đánh giá (Số sao và bình luận)
     * @param userId ID người dùng đang gửi yêu cầu
     * @return ReviewResponse Thông tin đánh giá vừa tạo
     */
    @Override
    @Transactional
    public ReviewResponse createReview(String courseId, ReviewRequest request, String userId) {
        Enrollment enrollment = enrollmentRepository.findByLearnerIdAndCourseId(userId, courseId)
                .orElseThrow(() -> new AppException(EnrollmentErrorCode.UNAUTHORIZED_ACCESS));

        // Kiểm tra xem đã tồn tại đánh giá nào (bao gồm cả đã xóa mềm) chưa
        // Check if any review exists (including soft-deleted ones)
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

        return mapToResponse(review);
    }

    /**
     * Lấy danh sách đánh giá của khóa học hỗ trợ phân trang.
     * Tự động ẩn các đánh giá đã bị xóa mềm nhờ cơ chế @SQLRestriction.
     *
     * @param courseId ID khóa học
     * @param pageable Cấu hình phân trang
     * @return Page<ReviewResponse> Trang danh sách các đánh giá
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getCourseReviews(String courseId, Pageable pageable) {
        return reviewRepository.findAllByCourseId(courseId, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Học viên tự thực hiện xóa đánh giá của mình (Xóa mềm - Soft Delete).
     * Đảm bảo tính bảo mật, chỉ chính chủ nhân đánh giá mới được quyền xóa.
     *
     * @param reviewId ID đánh giá
     * @param userId ID người dùng gửi yêu cầu
     */
    @Override
    @Transactional
    public void deleteReview(String reviewId, String userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(EnrollmentErrorCode.PROGRESS_NOT_FOUND));

        if (!review.getEnrollment().getLearnerId().equals(userId)) {
            throw new AppException(EnrollmentErrorCode.UNAUTHORIZED_ACCESS);
        }

        // Thực hiện xóa mềm: Đánh dấu cờ isDeleted là true và lưu lại
        review.setIsDeleted(true);
        reviewRepository.save(review);
    }

    /**
     * Cập nhật nội dung đánh giá (Số sao và bình luận).
     * Chỉ cho phép chính học viên tạo đánh giá đó được sửa đổi.
     *
     * @param reviewId ID đánh giá cần sửa
     * @param request Nội dung cập nhật mới
     * @param userId ID người dùng gửi yêu cầu
     * @return ReviewResponse Thông tin đánh giá sau khi cập nhật thành công
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

        return mapToResponse(review);
    }

    /**
     * Quản trị viên (Admin) thực hiện xóa đánh giá (Xóa mềm - Soft Delete).
     * Không yêu cầu xác thực ID người dùng là chủ sở hữu.
     *
     * @param reviewId ID đánh giá
     */
    @Override
    @Transactional
    public void adminDeleteReview(String reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(EnrollmentErrorCode.PROGRESS_NOT_FOUND));

        // Quản trị viên xóa mềm đánh giá
        review.setIsDeleted(true);
        reviewRepository.save(review);
    }

    /**
     * Chuyển đổi thực thể Review sang DTO ReviewResponse.
     */
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
