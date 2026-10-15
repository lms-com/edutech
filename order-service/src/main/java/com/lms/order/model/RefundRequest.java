package com.lms.order.model;

import com.lms.common.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "refund_requests")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RefundRequest extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(name = "order_id", length = 36, nullable = false)
    String orderId;

    @Column(name = "learner_id", length = 36, nullable = false)
    String learnerId;

    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    BigDecimal amount;

    @Column(name = "currency_code", length = 3, nullable = false)
    @Builder.Default
    String currencyCode = "VND";

    @Column(name = "reason", length = 255, nullable = false)
    String reason;

    @Column(name = "detail", columnDefinition = "TEXT")
    String detail;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    RefundStatus status = RefundStatus.PENDING;

    @Column(name = "admin_note", columnDefinition = "TEXT")
    String adminNote;

    @Column(name = "resolved_by", length = 36)
    String resolvedBy;

    @Column(name = "resolved_at")
    Instant resolvedAt;
}
