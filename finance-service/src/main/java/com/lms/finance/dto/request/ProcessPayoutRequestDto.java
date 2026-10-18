package com.lms.finance.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Request body for Aprrove/Reject Payout Request")
public class ProcessPayoutRequestDto {

    @Schema(description = "Payout Id", example = "payout-inst01-001")
    @NotEmpty(message = "PayoutId cannot be empty")
    String payoutId;

    @Schema(description = "Bank reference number", example = "bank-ref-no-01")
    String bankReferenceNo;

    @Schema(description = "Reject reason", example = "I don't like")
    String rejectReason;

    @Schema(description = "Transfer time", example = "2026-10-08T14:50:42.104")
    LocalDateTime transferAt;
}
