package com.lms.enrollment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Thống kê tổng hợp đánh giá của khóa học: điểm trung bình, tổng lượt đánh giá và phân bố từ 1 đến 5 sao.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingSummaryResponse {
    private String courseId;
    private Double averageRating;
    private Long totalReviews;
    private Map<Integer, Long> starDistribution;
}
