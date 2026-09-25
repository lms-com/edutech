package com.lms.finance.service.impl;

import com.lms.common.exception.AppException;
import com.lms.finance.dto.response.BalanceInstructorResponse;
import com.lms.finance.entity.BalanceHistory;
import com.lms.finance.entity.InstructorBalance;
import com.lms.finance.entity.RevenueShare;
import com.lms.finance.enums.EntryType;
import com.lms.finance.enums.ReferenceType;
import com.lms.finance.enums.RevenueShareStatus;
import com.lms.finance.enums.TransactionType;
import com.lms.finance.exception.FinanceErrorCode;
import com.lms.finance.repository.BalanceHistoryRepository;
import com.lms.finance.repository.InstructorBalanceRepository;
import com.lms.finance.repository.RevenueShareRepository;
import com.lms.finance.service.InstructorBalanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InstructorBalanceServiceImpl implements InstructorBalanceService {
    private final InstructorBalanceRepository balanceRepository;
    private final BalanceHistoryRepository balanceHistoryRepository;
    private final RevenueShareRepository revenueShareRepository;

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

    @Transactional
    @Override
    public void releaseRevenue (List<RevenueShare> releasableRevenues) {
        // Tao Set of instructorId from revenueShares
        Set<String> instructorIdSet = releasableRevenues.stream()
                .map(RevenueShare::getInstructorId)
                .collect(Collectors.toSet());

        // Lay InstructorBalance tu Set of instructorId
        List<InstructorBalance> balances = balanceRepository.findByInstructorIdIn((instructorIdSet));

        // Tao Map instructorId: InstructorBalance
        Map<String, InstructorBalance> instructorBalanceMap = balances.stream()
                .collect(Collectors.toMap(InstructorBalance::getInstructorId, b -> b));

        // Tao List of BalanceHistory empty
        List<BalanceHistory> histories = new ArrayList<>();

        // Duyet qua tung revenueShare
        for (RevenueShare share : releasableRevenues) {
            InstructorBalance balance = instructorBalanceMap.get(share.getInstructorId());

            if (balance == null) {
                log.warn("⚠️ Instructor {} has no balance yet", share.getInstructorId());
                // Co the them thao tac tao InstructorBalance cho giang vien
                continue;
            }
            BigDecimal amount = share.getInstructorAmount();
            BigDecimal pendingBalanceBefore = balance.getPendingBalance();
            BigDecimal availableBalanceBefore = balance.getAvailableBalance();
            BigDecimal blockedBalanceBefore = balance.getBlockedBalance();

            // InstructorBalance chuyen tu pending sang available
            balance.releasePendingToAvailable(amount);

            // Tao Balance history
            String note = String.format("Giải phóng số dư đóng băng %s %s cho khóa học [ID: %s] - Đơn hàng #%s",
                    amount, share.getCurrencyCode(), share.getCourseId(), share.getOrderId());
            histories.add(BalanceHistory.createLog(
                    share.getInstructorId(),
                    balance,
                    EntryType.CREDIT,
                    TransactionType.RELEASE_REVENUE,
                    amount,
                    pendingBalanceBefore,
                    availableBalanceBefore,
                    blockedBalanceBefore,
                    share.getId(),
                    ReferenceType.REVENUE_SHARE.name(),
                    note
            ));

            // Doi status cua revenueShare
            share.setStatus(RevenueShareStatus.RELEASED);
        }
        // Save tat ca vao DB
        balanceRepository.saveAll(balances);
        balanceHistoryRepository.saveAll(histories);
        revenueShareRepository.saveAll(releasableRevenues);
        log.info("👌✅ Successfully released {} revenue share records.", releasableRevenues.size());
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
