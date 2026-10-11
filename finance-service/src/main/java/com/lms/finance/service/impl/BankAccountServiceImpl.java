package com.lms.finance.service.impl;

import com.lms.common.exception.AppException;
import com.lms.finance.dto.request.UpdateBankAccountRequest;
import com.lms.finance.dto.response.BankAccountResponse;
import com.lms.finance.entity.BankAccount;
import com.lms.finance.exception.FinanceErrorCode;
import com.lms.finance.repository.BankAccountRepository;
import com.lms.finance.service.BankAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BankAccountServiceImpl implements BankAccountService {

    private final BankAccountRepository bankAccountRepository;

    @Override
    @Transactional(readOnly = true)
    public BankAccountResponse getPrimaryBankAccount(String instructorId) {
        return bankAccountRepository.findByInstructorIdAndIsPrimaryTrue(instructorId)
                .or(() -> bankAccountRepository.findFirstByInstructorIdOrderByCreatedAtDesc(instructorId))
                .map(this::mapToResponse)
                .orElse(null);
    }

    @Override
    @Transactional
    public BankAccountResponse saveOrUpdateBankAccount(String instructorId, UpdateBankAccountRequest request) {
        String cleanBankCode = request.getBankCode().trim().toUpperCase();
        String cleanAccountNumber = request.getAccountNumber().trim().replaceAll("\\s+", "");
        String cleanAccountName = request.getAccountName().trim().toUpperCase();

        // Tìm tài khoản primary hiện tại (nếu có)
        BankAccount existingAccount = bankAccountRepository.findByInstructorIdAndIsPrimaryTrue(instructorId)
                .or(() -> bankAccountRepository.findFirstByInstructorIdOrderByCreatedAtDesc(instructorId))
                .orElse(null);

        if (existingAccount != null) {
            existingAccount.setBankCode(cleanBankCode);
            existingAccount.setAccountNumber(cleanAccountNumber);
            existingAccount.setAccountName(cleanAccountName);
            existingAccount.setIsPrimary(true);
            BankAccount saved = bankAccountRepository.save(existingAccount);
            log.info("Cập nhật tài khoản ngân hàng thành công cho giảng viên {}: {} - {}", instructorId, cleanBankCode, cleanAccountNumber);
            return mapToResponse(saved);
        }

        // Đảm bảo các tài khoản cũ (nếu có) không còn là primary
        List<BankAccount> allAccounts = bankAccountRepository.findAllByInstructorId(instructorId);
        for (BankAccount acc : allAccounts) {
            acc.setIsPrimary(false);
        }
        if (!allAccounts.isEmpty()) {
            bankAccountRepository.saveAll(allAccounts);
        }

        BankAccount newAccount = BankAccount.builder()
                .instructorId(instructorId)
                .bankCode(cleanBankCode)
                .accountNumber(cleanAccountNumber)
                .accountName(cleanAccountName)
                .isPrimary(true)
                .build();

        BankAccount saved = bankAccountRepository.save(newAccount);
        log.info("Tạo mới tài khoản ngân hàng cho giảng viên {}: {} - {}", instructorId, cleanBankCode, cleanAccountNumber);
        return mapToResponse(saved);
    }

    private BankAccountResponse mapToResponse(BankAccount account) {
        return BankAccountResponse.builder()
                .id(account.getId())
                .instructorId(account.getInstructorId())
                .bankCode(account.getBankCode())
                .accountNumber(account.getAccountNumber())
                .accountName(account.getAccountName())
                .isPrimary(account.getIsPrimary())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
}
