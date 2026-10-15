package com.lms.notification.event.payload;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PayoutEventMessage {
    String payoutId;
    String instructorId;
    BigDecimal amount;
    String status;
    String bankCode;
    String accountNumber;
    String note;
    Instant timestamp;
}
