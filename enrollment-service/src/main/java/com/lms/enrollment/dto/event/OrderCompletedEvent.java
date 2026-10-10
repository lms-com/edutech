package com.lms.enrollment.dto.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderCompletedEvent {
    String orderId;
    String learnerId;
    List<OrderItemDto> items;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OrderItemDto {
        String courseId;
        String instructorId;
        BigDecimal finalPrice;
        String paymentCurrency;
        BigDecimal commissionRate;
    }
}
