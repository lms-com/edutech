package com.lms.analytics.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminOverviewResponse {
    BigDecimal totalGmvThisMonth;     // Tổng giá trị giao dịch
    BigDecimal netRevenueThisMonth;   // Doanh thu thực của sàn (Platform Fee)
    Integer totalUsers;               // Tổng số User (Cần gọi User-Service)
    Integer totalCourses;             // Tổng số khóa học trên sàn (Cần gọi Course-Service)
}