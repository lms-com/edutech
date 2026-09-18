package com.lms.finance.service;

import com.lms.finance.entity.InstructorBalance;

import java.math.BigDecimal;

public interface InstructorBalanceService {
    void depositToPendingBalance(BigDecimal amount, String currencyCode, String instructorId, String referenceId, String referenceType, String note) ;
}
