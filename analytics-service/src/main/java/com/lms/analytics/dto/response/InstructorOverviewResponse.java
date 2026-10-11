package com.lms.analytics.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstructorOverviewResponse {
    private BigDecimal availableBalance;      // Lấy từ finance-service
    private BigDecimal totalRevenueThisMonth; // Tính từ analytics_db
    private Integer totalEnrollments;         // Tính từ analytics_db
    private Integer totalCourses;             // Lấy từ course-service
    private Double averageRating;             // Lấy từ course-service
}
