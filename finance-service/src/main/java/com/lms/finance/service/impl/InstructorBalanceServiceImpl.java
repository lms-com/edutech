package com.lms.finance.service.impl;

import com.lms.common.exception.AppException;
import com.lms.finance.dto.response.BalanceInstructorResponse;
import com.lms.finance.entity.BalanceHistory;
import com.lms.finance.entity.InstructorBalance;
import com.lms.finance.enums.EntryType;
import com.lms.finance.enums.TransactionType;
import com.lms.finance.exception.FinanceErrorCode;
import com.lms.finance.repository.BalanceHistoryRepository;
import com.lms.finance.repository.InstructorBalanceRepository;
import com.lms.finance.service.InstructorBalanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class InstructorBalanceServiceImpl implements InstructorBalanceService {
    private final InstructorBalanceRepository balanceRepository;
    private final BalanceHistoryRepository balanceHistoryRepository;

    @Override
    public void depositToPendingBalance(
            BigDecimal amount,
            String currencyCode,
            String instructorId,
            String referenceId,
            String referenceType,
            String note
    ) {
        log.info("👇 Come in Function depositing to pending balance");
        InstructorBalance instructorBalance = balanceRepository.findByInstructorId((instructorId))
                .orElseThrow(() -> new AppException(FinanceErrorCode.INSTRUCTOR_BALANCE_NOT_EXISTS,
                        String.format("Instructor balance not exists for instructorId: %s", instructorId)
                ));
        BigDecimal pendingBalanceBefore = instructorBalance.getPendingBalance();
        instructorBalance.depositComission(amount);
        balanceHistoryRepository.save(BalanceHistory.createLog(
                instructorId,
                instructorBalance,
                EntryType.CREDIT,
                TransactionType.DEPOSIT_FROM_ORDER,
                amount,
                pendingBalanceBefore,
                instructorBalance.getAvailableBalance(),
                instructorBalance.getBlockedBalance(),
                referenceId,
                referenceType,
                note
        ));
        balanceRepository.save(instructorBalance);
    }


    @Override
    public BalanceInstructorResponse getMyBalances(String userId) {
        InstructorBalance balance = balanceRepository.findByInstructorId(userId)
                .orElseThrow(() -> new AppException(FinanceErrorCode.INSTRUCTOR_BALANCE_NOT_EXISTS));

        return BalanceInstructorResponse.builder()
                .actualBalance(balance.getActualBalance())
                .availableBalance(balance.getAvailableBalance())
                .blockedBalance(balance.getBlockedBalance())
                .pendingBalance(balance.getPendingBalance())
                .build();
    }
}
