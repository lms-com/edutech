package com.lms.finance.service;

import com.lms.finance.dto.response.BalanceInstructorResponse;

import java.math.BigDecimal;

public interface InstructorBalanceService {
    void depositToPendingBalance(BigDecimal amount, String currencyCode, String instructorId, String referenceId, String referenceType, String note) ;
    BalanceInstructorResponse getMyBalances(String userId);
}
