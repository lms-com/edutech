package com.lms.course.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * API 35: Response nội bộ cấp thông tin giá cho Order Service.
 */
@Data
@Builder
public class CoursePriceResponse {
    private String courseId;
    private String title;
    private BigDecimal basePrice;
    private String currencyCode;
    private BigDecimal overrideCommissionRate;
    private String instructorId;
    private String status;
}
