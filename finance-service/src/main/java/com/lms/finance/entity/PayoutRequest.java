package com.lms.finance.entity;

import com.lms.finance.enums.PayoutStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payout_requests", indexes = {
        @Index(name = "idx_payout_instructor", columnList = "instructor_id, created_at DESC"),
        @Index(name = "idx_payout_status", columnList = "status, created_at DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@EntityListeners(AuditingEntityListener.class)
public class PayoutRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, updatable = false, nullable = false)
    String id;

    @Column(name = "instructor_id", length = 36, nullable = false)
    String instructorId;

    @Column(precision = 15, scale = 2, nullable = false)
    BigDecimal amount;

    @Column(name = "currency_code", length = 3, nullable = false)
    @Builder.Default
    String currencyCode = "VND";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "ENUM('PENDING','SUCCESS','REJECTED')")
    @Builder.Default
    PayoutStatus status = PayoutStatus.PENDING;

    // ── Snapshot thông tin ngân hàng TẠI THỜI ĐIỂM tạo lệnh ──
    // Không dùng FK sang bank_accounts — nếu Instructor đổi TK sau, lệnh cũ vẫn đúng
    @Column(name = "bank_code", length = 20, nullable = false)
    String bankCode;

    @Column(name = "account_number", length = 50, nullable = false)
    String accountNumber;

    @Column(name = "account_name", length = 200, nullable = false)
    String accountName;

    /** Admin ID thực hiện duyệt/từ chối */
    @Column(name = "processed_by", length = 36)
    String processedBy;

    @Column(name = "processed_at")
    Instant processedAt;

    @Column(name = "reject_reason", length = 500)
    String rejectReason;

    /** Số bút toán chuyển khoản thực tế — Admin điền sau khi CK xong */
    @Column(name = "bank_reference_no", length = 100)
    String bankReferenceNo;

}
