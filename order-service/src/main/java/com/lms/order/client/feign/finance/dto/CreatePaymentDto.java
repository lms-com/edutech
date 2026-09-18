package com.lms.order.client.finance.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreatePaymentDto {
    String learnerId;
    String orderId;
    long amount;
    String currencyCode;
    String paymentMethod = "VNPAY";
}
