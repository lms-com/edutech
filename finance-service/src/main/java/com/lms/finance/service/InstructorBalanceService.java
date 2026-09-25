package com.lms.finance.service;

import com.lms.finance.dto.response.BalanceInstructorResponse;
import com.lms.finance.entity.RevenueShare;

import java.math.BigDecimal;
import java.util.List;

public interface InstructorBalanceService {
    void depositToPendingBalance(BigDecimal amount, String currencyCode, String instructorId, String referenceId, String referenceType, String note) ;
    void releaseRevenue (List<RevenueShare> releasableRevenues);
    BalanceInstructorResponse getMyBalances(String userId);
}
