package com.lms.order.dto.message;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentProcessMessage {
    String orderId;
    String learnerId;
    long amount;
    String status;
    String transactionNo;
    Instant paidAt;
    // ko biet du attribute chua
}
