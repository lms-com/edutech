package com.lms.finance.service.impl;

import com.lms.common.exception.AppException;
import com.lms.common.util.TimeConverter;
import com.lms.finance.dto.message.PayoutEventMessage;
import com.lms.finance.dto.request.PayoutFilterRequest;
import com.lms.finance.dto.request.ProcessPayoutRequestDto;
import com.lms.finance.dto.response.PayoutRequestResponse;
import com.lms.finance.entity.BalanceHistory;
import com.lms.finance.entity.BankAccount;
import com.lms.finance.entity.InstructorBalance;
import com.lms.finance.entity.PayoutRequest;
import com.lms.finance.enums.EntryType;
import com.lms.finance.enums.PayoutStatus;
import com.lms.finance.enums.ReferenceType;
import com.lms.finance.enums.TransactionType;
import com.lms.finance.exception.FinanceErrorCode;
import com.lms.finance.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static com.lms.finance.config.RabbitMQConfig.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayoutServiceImpl implements PayoutService {
    final InstructorBalanceRepository balanceRepository;
    final PayoutRequestRepository payoutRepository;
    final BankAccountRepository bankAccountRepository;
    final BalanceHistoryRepository historyRepository;
    final RabbitTemplate rabbitTemplate;

    @Value("${application.finance.minimum-payout-amount}")
    String MIN_PAYOUT_AMOUNT;
    @Value("${application.finance.default-currency}")
    String DEFAULT_CURRENCY;

    @Transactional
    @Override
    public PayoutRequestResponse createPayoutRequest (String instructorId, BigDecimal amount) {
        // Check the minimum payout amount:
        if (amount.compareTo(new BigDecimal(MIN_PAYOUT_AMOUNT)) < 0) {
            throw new AppException(FinanceErrorCode.INVALID_AMOUNT, "Minimum amount of payout must be " + MIN_PAYOUT_AMOUNT + "VND");
        }
        // Get Instructor balance:
        InstructorBalance balance = balanceRepository.findByInstructorId(instructorId)
                .orElseThrow(() -> new AppException(FinanceErrorCode.INSTRUCTOR_BALANCE_NOT_EXISTS));

        // Check available balance is enough?
        BigDecimal availableBalance = balance.getAvailableBalance();
        if (availableBalance.compareTo(amount) < 0) {
            throw new AppException(FinanceErrorCode.INSUFFICTION_BALANCE, "Insufficient wallet balance. Please enter a lower amount.");
        }

        // Get the primary bank account:
        BankAccount bankAccount = bankAccountRepository.findByInstructorIdAndIsPrimaryTrue(instructorId)
                .orElseThrow(() -> new AppException(FinanceErrorCode.BANK_ACCOUNT_NOT_EXISTS, "Not found bank account for instructor Id: " + instructorId));

        // Save the balance before the change for logging
        BigDecimal availableAmount = balance.getAvailableBalance();
        BigDecimal blockedAmount = balance.getBlockedBalance();
        BigDecimal pendingAmount = balance.getPendingBalance();

        // Block fund for payout:
        balance.blockFundsForPayout(amount);
        balanceRepository.save(balance);

        // Create payout request
        String bankCode = bankAccount.getBankCode();
        String accountNumber = bankAccount.getAccountNumber();
        String accountName = bankAccount.getAccountName();

        PayoutRequest payoutRequest = payoutRepository.save(PayoutRequest.builder()
                .instructorId(instructorId)
                .amount(amount)
                .currencyCode(DEFAULT_CURRENCY)
                .status(PayoutStatus.PENDING)
                .bankCode(bankCode)
                .accountNumber(accountNumber)
                .accountName(accountName)
                .build()
        );

        // Record new balance history:
        String note = String.format("Khóa số tiền %s để xử lí lệnh rút tiền cho giảng viên %s", amount, instructorId);
        historyRepository.save(BalanceHistory.createLog(
                instructorId,
                balance,
                EntryType.DEBIT,
                TransactionType.BLOCK_FOR_PAYOUT,
                amount,
                pendingAmount,
                availableAmount,
                blockedAmount,
                payoutRequest.getId(),
                ReferenceType.PAYOUT.name(),
                note
        ));

        // Push message:
        PayoutEventMessage message = createPayoutEventMessage(payoutRequest, note);
        rabbitTemplate.convertAndSend(PAYOUT_EXCHANGE, PAYOUT_PENDING_ROUTING_KEY, message);

        LocalDateTime createdAt = payoutRequest.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime();
        return toResponse(payoutRequest);
    }

    @Transactional
    @Override
    public void approvePayoutRequest(String adminId, ProcessPayoutRequestDto processRequest) {
        String payoutId = processRequest.getPayoutId();
        String bankReferenceNo = processRequest.getBankReferenceNo();
        // Get payoutRequest:
        PayoutRequest payoutRequest = payoutRepository.findById(payoutId)
                .orElseThrow(() -> new AppException(FinanceErrorCode.PAYOUT_REQUEST_NOT_EXISTS));
        String instructorId = payoutRequest.getInstructorId();
        log.info("👨‍🎓👨‍🎓 Instructor Id: " + instructorId);
        BigDecimal amount = payoutRequest.getAmount();

        // Get Instructor Balance
        InstructorBalance balance = balanceRepository.findByInstructorId(instructorId)
                .orElseThrow(() -> new AppException(FinanceErrorCode.INSTRUCTOR_BALANCE_NOT_EXISTS));

        // Check whether payoutStatus is Pending:
        if (!payoutRequest.getStatus().equals(PayoutStatus.PENDING)) {
            throw new AppException(FinanceErrorCode.INVALID_PAYOUT_REQUEST_STATUS, "Only requests that are in the PENDING status can be approved");
        }

        // Save the balance before the change for logging
        BigDecimal availableAmount = balance.getAvailableBalance();
        BigDecimal blockedAmount = balance.getBlockedBalance();
        BigDecimal pendingAmount = balance.getPendingBalance();

        // Call and execute approving payout method of InstructorBalance instance:
        balance.completePayout(amount);
        balanceRepository.save(balance);

        // update payout request to completed:
        payoutRequest.setStatus(PayoutStatus.SUCCESS);
        payoutRequest.setProcessedBy(adminId);
        payoutRequest.setProcessedAt(Instant.now());
        payoutRequest.setBankReferenceNo(bankReferenceNo);
        payoutRepository.save(payoutRequest);

        // Record new log
        String accountName = payoutRequest.getAccountName();
        String accountNumber = payoutRequest.getAccountNumber();
        String bankCode = payoutRequest.getBankCode();

        String note = String.format("Chuyển khoản thành công đến người nhận: %s\nSTK: %s\nMã ngân hàng: %s\n MGD: %s",
                accountName, accountNumber, bankCode, bankReferenceNo);

        historyRepository.save(BalanceHistory.createLog(
                instructorId,
                balance,
                EntryType.DEBIT,
                TransactionType.WITHDRAW_SUCCESS,
                amount,
                pendingAmount,
                availableAmount,
                blockedAmount,
                bankReferenceNo,
                ReferenceType.PAYOUT.name(),
                note
        ));

        // Push message to broker:
        PayoutEventMessage message = createPayoutEventMessage(payoutRequest, note);
        rabbitTemplate.convertAndSend(PAYOUT_EXCHANGE, PAYOUT_SUCCESS_ROUTING_KEY, message);
    }

    @Transactional
    @Override
    public void rejectPayoutRequest(String adminId, ProcessPayoutRequestDto processRequest) {
        String payoutId = processRequest.getPayoutId();
        String rejectReason = processRequest.getRejectReason();

        PayoutRequest payoutRequest = payoutRepository.findById(payoutId)
                .orElseThrow(() -> new AppException(FinanceErrorCode.PAYOUT_REQUEST_NOT_EXISTS));

        if (!payoutRequest.getStatus().equals(PayoutStatus.PENDING)) {
            throw new AppException(FinanceErrorCode.INVALID_PAYOUT_REQUEST_STATUS, "Only requests that are in the PENDING status can be rejected");
        }

        payoutRequest.setStatus(PayoutStatus.REJECTED);
        payoutRequest.setProcessedBy(adminId);
        payoutRequest.setProcessedAt(Instant.now());
        payoutRequest.setRejectReason(rejectReason);
        payoutRepository.save(payoutRequest);

        String instructorId = payoutRequest.getInstructorId();
        BigDecimal amount = payoutRequest.getAmount();

        InstructorBalance balance = balanceRepository.findByInstructorId(instructorId)
                .orElseThrow(() -> new AppException(FinanceErrorCode.INSTRUCTOR_BALANCE_NOT_EXISTS));

        BigDecimal availableAmount = balance.getAvailableBalance();
        BigDecimal blockedAmount = balance.getBlockedBalance();
        BigDecimal pendingAmount = balance.getPendingBalance();

        balance.rejectPayout(amount);
        balanceRepository.save(balance);

        String note = String.format("Từ chối rút tiền: %s", rejectReason);
        historyRepository.save(BalanceHistory.createLog(
                instructorId,
                balance,
                EntryType.CREDIT,
                TransactionType.WITHDRAW_REJECTED,
                amount,
                pendingAmount,
                availableAmount,
                blockedAmount,
                payoutRequest.getId(),
                ReferenceType.PAYOUT.name(),
                note
        ));

        PayoutEventMessage message = createPayoutEventMessage(payoutRequest, note);
        rabbitTemplate.convertAndSend(PAYOUT_EXCHANGE, PAYOUT_REJECTED_ROUTING_KEY, message);
    }

    private PayoutEventMessage createPayoutEventMessage(PayoutRequest payoutRequest, String note) {
        return PayoutEventMessage.builder()
                .payoutId(payoutRequest.getId())
                .instructorId(payoutRequest.getInstructorId())
                .amount(payoutRequest.getAmount())
                .status(payoutRequest.getStatus().name())
                .bankCode(payoutRequest.getBankCode())
                .accountNumber(payoutRequest.getAccountNumber())
                .note(note)
                .timestamp(Instant.now())
                .build();
    }


    @Override
    public Page<PayoutRequestResponse> getMyPayoutRequests(String instructorId, PayoutFilterRequest filter) {
        // Kiem tra hanh dong trai phep -> user dang ngo:
        if (filter.getInstructorId() != null){
            throw new RuntimeException("You dont have permission to use this filter. Be careful, You have been added to the suspect/warning list.");
        }
        // Tao Page:
        Sort sort = Sort.by(filter.getSortDir().equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC, filter.getSortBy());
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), sort);

        filter.setInstructorId(instructorId);
        filter.setAdminId(null);
        Specification<PayoutRequest> spec = PayoutRequestSpecification.buildSpecification(filter);

        return payoutRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Override
    public Page<PayoutRequestResponse> getPayoutRequestsForAdmin(PayoutFilterRequest filter) {
        // Tao Pageable:
        Sort sort = Sort.by(filter.getSortDir().equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC, filter.getSortBy());
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), sort);

        Specification<PayoutRequest> spec = PayoutRequestSpecification.buildSpecification(filter);
        return payoutRepository.findAll(spec, pageable).map(this::toResponse);
    }

    private PayoutRequestResponse toResponse (PayoutRequest payoutRequest) {
        return PayoutRequestResponse.builder()
                .payoutId(payoutRequest.getId())
                .amount(payoutRequest.getAmount())
                .status(payoutRequest.getStatus().name())
                .bankCode(payoutRequest.getBankCode())
                .accountNumber(payoutRequest.getAccountNumber())
                .accountName(payoutRequest.getAccountName())
                .createdAt(TimeConverter.toLocalDateTime(payoutRequest.getCreatedAt(), ZoneId.systemDefault()))
                .processedAt(TimeConverter.toLocalDateTime(payoutRequest.getProcessedAt(), ZoneId.systemDefault()))
                .build();
    }
}