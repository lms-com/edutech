package com.lms.finance.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RevenueChartPointResponse {
    String period;         // Mốc thời gian dạng ISO: "2026-05"
    String label;          // Nhãn hiển thị giao diện: "T05/2026"
    BigDecimal revenue;    // Doanh thu giảng viên nhận được (VND)
    BigDecimal grossSales; // Tổng giá trị doanh số khóa học (VND)
    long orderCount;       // Số lượt mua / giao dịch
}
