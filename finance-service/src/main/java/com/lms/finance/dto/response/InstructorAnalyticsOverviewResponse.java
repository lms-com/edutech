package com.lms.finance.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InstructorAnalyticsOverviewResponse {
    long totalStudents;          // Tổng học viên đã mua khóa của giảng viên
    long totalCourses;           // Tổng số khóa học giảng viên đang có
    BigDecimal thisMonthRevenue; // Doanh thu giảng viên nhận trong tháng này
    BigDecimal lastMonthRevenue; // Doanh thu tháng trước
    double growthRate;            // Tỷ lệ tăng trưởng doanh thu so với tháng trước (%)
    long thisMonthSales;         // Số lượt mua trong tháng này
    BigDecimal availableBalance; // Số dư ví khả dụng hiện tại
    BigDecimal pendingBalance;   // Số dư ví đang tạm khóa đối soát
    BigDecimal actualBalance;    // Tổng số dư thực tế
    BigDecimal allTimeRevenue;   // Tổng doanh thu trọn đời
}
