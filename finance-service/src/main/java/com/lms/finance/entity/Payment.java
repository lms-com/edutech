package com.lms.finance.entity;

import com.lms.common.model.AuditableEntity;
import com.lms.finance.enums.PaymentMethod;
import com.lms.finance.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payments_order_id", columnList = "order_id"),
        @Index(name = "idx_payments_learner_status", columnList = "learner_id, status"),
        @Index(name = "idx_payments_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Payment extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, updatable = false, nullable = false)
    String id;

    @Column(name = "order_id", length = 36, nullable = false)
    String orderId;

    @Column(name = "learner_id", length = 36, nullable = false)
    String learnerId;

    @Column(nullable = false)
    Long amount;

    @Column(name = "currency_code", length = 10, nullable = false)
    String currencyCode;

    // VARCHAR trong DB — dùng enum Java để an toàn, lưu dạng String
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20, nullable = false)
    PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "ENUM('PROCESSING','SUCCESS','FAILED','REFUNDED')")
    PaymentStatus status;

    @Column(name = "return_url", length = 500)
    String returnUrl;

    @Column(name = "paid_at")
    Instant paidAt;

    @Column(name = "is_deleted", nullable = false)
    Boolean deleted;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<PaymentTransaction> transactions = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (this.currencyCode == null) this.currencyCode = "VND";
        if (this.status == null) this.status = PaymentStatus.PROCESSING;
        if (this.deleted == null) this.deleted = false;
    }
}
