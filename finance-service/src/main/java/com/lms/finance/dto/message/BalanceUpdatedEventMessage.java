package com.lms.finance.dto.message;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class BalanceUpdatedEventMessage {
    private String instructorId;
    private BigDecimal availableBalance;
}
