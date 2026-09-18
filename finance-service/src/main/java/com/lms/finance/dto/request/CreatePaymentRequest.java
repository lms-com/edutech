package com.lms.finance.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreatePaymentRequest {
    String learnerId;
    String orderId;
    long amount;
    String currencyCode;
    String paymentMethod = "VNPAY";
    String paymentRef;
}
