package com.lms.finance.service;

import com.lms.finance.dto.request.UpdateBankAccountRequest;
import com.lms.finance.dto.response.BankAccountResponse;

public interface BankAccountService {
    BankAccountResponse getPrimaryBankAccount(String instructorId);
    BankAccountResponse saveOrUpdateBankAccount(String instructorId, UpdateBankAccountRequest request);
}
