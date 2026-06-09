package com.lms.finance.entity;

import com.lms.common.model.AuditableEntity;
import com.lms.finance.enums.EntryType;
import com.lms.finance.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "balance_histories", indexes = {
        @Index(name = "idx_balance_histories_instructor", columnList = "instructor_id, created_at DESC"),
        @Index(name = "idx_balance_histories_type", columnList = "instructor_id, transaction_type"),
        @Index(name = "idx_balance_histories_reference", columnList = "reference_id, reference_type")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // JPA duoc tao, nhung ben ngoai thi khong
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@EntityListeners(AuditingEntityListener.class)
public class BalanceHistory {

    @Id
    @Column(length = 36, updatable = false, nullable = false)
    String id;

    @Column(name = "instructor_balance_id", length = 36, nullable = false)
    String instructorBalanceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false)
    EntryType entryType;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false,
            columnDefinition = "ENUM('DEPOSIT_FROM_ORDER','BLOCK_FOR_PAYOUT','WITHDRAW_SUCCESS','WITHDRAW_REJECTED','REFUND_DEDUCTION')")
    TransactionType transactionType;

    /**
     * Số tiền biến động.
     * DƯƠNG = cộng vào available_balance (VD: +5000000 khi nhận hoa hồng).
     * ÂM   = trừ khỏi available_balance (VD: -2000000 khi rút tiền).
     */
    @Column(precision = 15, scale = 2, nullable = false)
    BigDecimal amount;

    @Column(name = "currency_code", length = 3, nullable = false)
    String currencyCode;

    /** Snapshot available_balance TRƯỚC và SAU khi áp dụng giao dịch này */
    @Column(name = "pending_balance_before", precision = 15, scale = 2, nullable = false)
    BigDecimal pendingBalanceBefore = BigDecimal.ZERO;

    @Column(name = "pending_balance_after", precision = 15, scale = 2, nullable = false)
    BigDecimal pendingBalanceAfter = BigDecimal.ZERO;

    @Column(name = "available_balance_before", precision = 15, scale = 2, nullable = false)
    BigDecimal availableBalanceBefore = BigDecimal.ZERO;

    @Column(name = "available_balance_after", precision = 15, scale = 2, nullable = false)
    BigDecimal availableBalanceAfter = BigDecimal.ZERO;

    @Column(name = "blocked_balance_before", precision = 15, scale = 2, nullable = false)
    BigDecimal blockedBalanceBefore = BigDecimal.ZERO;

    @Column(name = "blocked_balance_after", precision = 15, scale = 2, nullable = false)
    BigDecimal blockedBalanceAfter = BigDecimal.ZERO;


    /** ID nguồn gốc: revenue_share_id / payout_request_id / payment_id */
    @Column(name = "reference_id", length = 36, nullable = false)
    String referenceId;

    /** Loại nguồn: REVENUE_SHARE / PAYOUT / PAYMENT */
    @Column(name = "reference_type", length = 50, nullable = false)
    String referenceType;

    /** Ghi chú thêm (VD: Admin ghi lý do từ chối) */
    @Column(length = 500)
    String note;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    public static BalanceHistory createLog (
            InstructorBalance wallet,
            EntryType entryType,
            TransactionType transactionType,
            BigDecimal amount,
            BigDecimal pendingBefore,
            BigDecimal availableBefore,
            BigDecimal blockedBefore,
            String referenceId,
            String referenceType,
            String note) {
        BalanceHistory newLog = new BalanceHistory();
        newLog.setId(UUID.randomUUID().toString());
        newLog.setInstructorBalanceId(wallet.getId());
        newLog.setEntryType(entryType);
        newLog.setTransactionType(transactionType);
        newLog.setAmount(amount);
        newLog.setCurrencyCode(wallet.getCurrencyCode());

        // // Chụp lại trạng thái TRƯỚC khi biến động
        newLog.setPendingBalanceBefore(pendingBefore);
        newLog.setAvailableBalanceBefore(availableBefore);
        newLog.setBlockedBalanceBefore(blockedBefore);

        // Chụp lại trạng thái SAU khi biến động (Lấy trực tiếp từ Object ví hiện tại)
        newLog.setPendingBalanceAfter(wallet.getPendingBalance());
        newLog.setAvailableBalanceAfter(wallet.getAvailableBalance());
        newLog.setBlockedBalanceAfter(wallet.getBlockedBalance());

        // Luu tham chieu truy vet nguyen nhan bien dong
        newLog.setReferenceId(referenceId);
        newLog.setReferenceType(referenceType);
        newLog.setNote(note);

        return newLog;
    }
}
