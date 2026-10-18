package com.lms.finance.dto.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderCompletedMessage {
    String orderId;
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