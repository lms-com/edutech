package com.lms.finance.dto.request;

import com.lms.finance.enums.BalanceType;
import com.lms.finance.enums.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Query parameters for filtering and paginating instructor balance history")
public class BalanceHistoryFilterRequest {

    @Schema(description = "Page number to retrieve (starts from 0)", example = "0")
    int page = 0;

    @Schema(description = "Number of records per page (max 100)", example = "20")
    int size = 20;

    @Schema(description = "Filter by specific wallet/balance type affected", example = "AVAILABLE")
    BalanceType balanceType;

    @Schema(description = "Filter by transaction action type", example = "WITHDRAW")
    TransactionType transactionType;

    @Schema(description = "Search by exact reference identifier (e.g., Payout Request ID, Order ID)", example = "PAYOUT_1002")
    String referenceId;

    @Schema(description = "Start date for filtering (yyyy-MM-dd)", example = "2026-09-01")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate startDate;

    @Schema(description = "End date for filtering (yyyy-MM-dd)", example = "2026-09-24")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate endDate;
}
