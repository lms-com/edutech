package com.lms.order.dev.dev_dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DevOrderResponse {
    String id;
    BigDecimal totalPrice;
    String status;
    List<Item> items = new ArrayList<>();

    @Data
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Item {
        String itemId;
        String courseName;
        BigDecimal originalPrice;
        BigDecimal discountAmount;
        BigDecimal commissionRate;
    }
}
