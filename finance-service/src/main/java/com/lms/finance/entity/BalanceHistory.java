package com.lms.finance.entity;

import com.lms.common.model.AuditableEntity;
import com.lms.finance.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "balance_histories", indexes = {
        @Index(name = "idx_balance_histories_instructor", columnList = "instructor_id, created_at DESC"),
        @Index(name = "idx_balance_histories_type", columnList = "instructor_id, transaction_type"),
        @Index(name = "idx_balance_histories_reference", columnList = "reference_id, reference_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BalanceHistory extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, updatable = false, nullable = false)
    String id;

    @Column(name = "instructor_id", length = 36, nullable = false)
    String instructorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false,
            columnDefinition = "ENUM('DEPOSIT_FROM_ORDER','BLOCK_FOR_PAYOUT','WITHDRAW_SUCCESS','WITHDRAW_REJECTED','REFUND_DEDUCTION')")
    TransactionType transactionType;

    /**
     * Số tiền biến động.
     * DƯƠNG = cộng vào available_balance (VD: +5000000 khi nhận hoa hồng).
     * ÂM   = trừ khỏi available_balance (VD: -2000000 khi rút tiền).
     */
    @Column(nullable = false)
    Long amount;

    @Column(name = "currency_code", length = 10, nullable = false)
    String currencyCode;

    /** Snapshot available_balance TRƯỚC khi áp dụng giao dịch này */
    @Column(name = "balance_before", nullable = false)
    Long balanceBefore;

    /** Snapshot available_balance SAU khi áp dụng giao dịch này */
    @Column(name = "balance_after", nullable = false)
    Long balanceAfter;

    /** ID nguồn gốc: revenue_share_id / payout_request_id / payment_id */
    @Column(name = "reference_id", length = 36)
    String referenceId;

    /** Loại nguồn: REVENUE_SHARE / PAYOUT / PAYMENT */
    @Column(name = "reference_type", length = 50)
    String referenceType;

    /** Ghi chú thêm (VD: Admin ghi lý do từ chối) */
    @Column(length = 500)
    String note;

    @Column(name = "is_deleted", nullable = false)
    Boolean deleted;

    @PrePersist
    public void prePersist() {
        if (this.currencyCode == null) this.currencyCode = "VND";
        if (this.deleted == null) this.deleted = false;
    }
}
