package com.lms.finance.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PayoutRequestResponse {
    String payoutId;
    String instructorId;
    BigDecimal amount;
    String status;
    String bankCode;
    String accountNumber;
    String accountName;
    String rejectReason;
    String bankReferenceNo;
    LocalDateTime createdAt;
    LocalDateTime processedAt;
}
