package com.lms.finance.dto.message;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
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
