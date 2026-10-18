package com.lms.finance.dto.response;

import com.lms.finance.enums.BalanceAction;
import com.lms.finance.enums.BalanceType;
import com.lms.finance.enums.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Details about a Balance History")
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BalanceHistoryInstructorResponse {
    @Schema(description = "Reference id of the original transaction", example = "payment_001")
    String referenceId;

    @Schema(description = "Type of transaction", example = "DEPOSIT_FROM_ORDER")
    TransactionType transactionType;

    @Schema(description = "the total amount of the main fluctuations in this transaction", example = "1520000.00")
    BigDecimal amount;

    @Schema(description = "Time of recording balance changes", example = "2026-209-24T11:25:44")
    LocalDateTime createdAt;

    @Schema(description = "Major volatility impact (always present)")
    Impact impact;

    @Schema(description = "Secondary volatility impact (Nuallable if not transfering in an internal wallet")
    Impact secondImpact;


    @Schema(description = "Details of the impact")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Impact {
        @Schema(description = "Type of balance impacted", example = "PENDING")
        BalanceType balanceType;

        @Schema(description = "Actions that decrease or increase", example = "DECREASE")
        BalanceAction action;

        @Schema(description = "The specific amount that changed", example = "1520000.00")
        BigDecimal amountChanged;
    }
}

// Ngày hôm nay tôilamamf chủ vanaj meệnh của tôi, tôi kểm soát năng lượng của mình, ngày hôm nay tôi làm những việc cần làm, nghĩ những việc cần nghĩ, làm những việc tuyệt vời nhất, tôi là người tuyệt vời, tôi là người thành công, tôi là người tử tế và tôi xứng đáng với điều tuyệt vời nhất