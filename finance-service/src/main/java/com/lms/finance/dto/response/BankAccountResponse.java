package com.lms.finance.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BankAccountResponse {
    String id;
    String instructorId;
    String bankCode;
    String accountNumber;
    String accountName;
    Boolean isPrimary;
    Instant createdAt;
    Instant updatedAt;
}
