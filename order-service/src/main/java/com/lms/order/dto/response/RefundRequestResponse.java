package com.lms.order.dto.response;

import com.lms.order.model.RefundStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RefundRequestResponse {
    String id;
    String orderId;
    String learnerId;
    BigDecimal amount;
    String currencyCode;
    String reason;
    String detail;
    RefundStatus status;
    String adminNote;
    String resolvedBy;
    Instant resolvedAt;
    Instant createdAt;
    Instant orderCreatedAt;
    List<RefundOrderItemDto> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class RefundOrderItemDto {
        String courseId;
        String courseName;
        String instructorId;
        BigDecimal finalPrice;
    }
}
