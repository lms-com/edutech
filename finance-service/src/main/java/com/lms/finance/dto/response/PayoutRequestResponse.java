package com.lms.finance.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PayoutRequestResponse {
    String payoutId;
    BigDecimal amount;
    String status;
    String bankCode;
    String accountNumber;
    String accountName;
    LocalDateTime createdAt;
    LocalDateTime processedAt;
}
