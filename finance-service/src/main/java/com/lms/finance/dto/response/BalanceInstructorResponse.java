package com.lms.finance.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BalanceInstructorResponse {
    BigDecimal actualBalance;
    BigDecimal availableBalance;
    BigDecimal blockedBalance;
    BigDecimal pendingBalance;
}
