package com.lms.order.dto.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
public class OrderCompletedMessage {
    String orderId;
    String learnerId;
    List<OrderItemDto> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderItemDto {
        String courseId;
        String instructorId;
        BigDecimal finalPrice;
        String paymentCurrency;
        BigDecimal commissionRate;
    }
}
