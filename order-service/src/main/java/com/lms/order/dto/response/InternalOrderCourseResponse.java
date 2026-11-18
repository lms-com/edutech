package com.lms.order.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalOrderCourseResponse {
    private String id;
    private String paymentCurrency;
    private String instructorId;
    private BigDecimal commissionRate;
    private BigDecimal finalPrice;
}
