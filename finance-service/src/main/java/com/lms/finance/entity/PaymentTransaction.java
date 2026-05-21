package com.lms.finance.entity;

import com.lms.common.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Entity
@Table(name = "payment_transactions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_gateway_transaction",
                        columnNames = {"gateway", "gateway_transaction_id"})
        },
        indexes = {
                @Index(name = "idx_payment_transactions_payment_id", columnList = "payment_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentTransaction extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, updatable = false, nullable = false)
    String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    Payment payment;

    @Column(name = "gateway_transaction_id", length = 100, nullable = false)
    String gatewayTransactionId;

    // VARCHAR trong DB — dễ mở rộng MOMO, ZALOPAY mà không ALTER TABLE
    @Column(length = 20, nullable = false)
    String gateway;

    @Column(name = "gateway_status", length = 50, nullable = false)
    String gatewayStatus;

    @Column(name = "gateway_response", columnDefinition = "JSON")
    String gatewayResponse;

    @Column(nullable = false)
    Long amount;

    @Column(name = "currency_code", length = 10, nullable = false)
    String currencyCode;

    @Column(name = "transacted_at")
    Instant transactedAt;

    @Column(name = "is_deleted", nullable = false)
    Boolean deleted;

    @PrePersist
    public void prePersist() {
        if (this.currencyCode == null) this.currencyCode = "VND";
        if (this.deleted == null) this.deleted = false;
    }
}
