package com.lms.finance.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CourseRevenueBreakdownResponse {
    String courseId;
    String courseTitle;
    BigDecimal basePrice;
    BigDecimal grossRevenue;      // Tổng doanh thu gộp từ học viên
    BigDecimal netRevenue;        // Doanh thu thực nhận của giảng viên sau khi trừ phí
    BigDecimal platformFee;       // Phí sàn / nền tảng giữ lại
    BigDecimal commissionRate;    // Tỷ lệ hoa hồng (VD: 0.70 = 70%)
    long salesCount;              // Số lượt mua khóa học
}
