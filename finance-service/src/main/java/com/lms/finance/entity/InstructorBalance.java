package com.lms.finance.entity;

import com.lms.common.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "instructor_balances",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_instructor_balances_instructor_id",
                        columnNames = "instructor_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InstructorBalance extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, updatable = false, nullable = false)
    String id;

    @Column(name = "instructor_id", length = 36, nullable = false, unique = true)
    String instructorId;

    @Column(name = "currency_code", length = 10, nullable = false)
    String currencyCode;

    /**
     * Số dư có thể rút. Đơn vị: VND (BIGINT — không dùng DECIMAL tránh lỗi làm tròn).
     * DB CHECK constraint đảm bảo >= 0.
     */
    @Column(name = "available_balance", nullable = false)
    Long availableBalance;

    /**
     * Số tiền đang bị đóng băng chờ Admin duyệt payout.
     * DB CHECK constraint đảm bảo >= 0.
     */
    @Column(name = "blocked_balance", nullable = false)
    Long blockedBalance;

    @Column(name = "is_deleted", nullable = false)
    Boolean deleted;

    // @Version đã có trong AuditableEntity — Optimistic Lock khi cộng/trừ số dư đồng thời

    @PrePersist
    public void prePersist() {
        if (this.currencyCode == null) this.currencyCode = "VND";
        if (this.availableBalance == null) this.availableBalance = 0L;
        if (this.blockedBalance == null) this.blockedBalance = 0L;
        if (this.deleted == null) this.deleted = false;
    }

    /** Tiện ích: available + blocked — không map xuống DB */
    @Transient
    public Long getTotalBalance() {
        return (availableBalance != null ? availableBalance : 0L)
                + (blockedBalance != null ? blockedBalance : 0L);
    }
}
