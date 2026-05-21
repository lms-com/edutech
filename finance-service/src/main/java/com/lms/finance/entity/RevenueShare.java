package com.lms.finance.entity;

import com.lms.common.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Entity
@Table(name = "revenue_shares",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_revenue_order_course",
                        columnNames = {"order_id", "course_id"})
        },
        indexes = {
                @Index(name = "idx_revenue_instructor", columnList = "instructor_id, created_at DESC"),
                @Index(name = "idx_revenue_order_id", columnList = "order_id"),
                @Index(name = "idx_revenue_course_id", columnList = "course_id"),
                @Index(name = "idx_revenue_created_at", columnList = "created_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RevenueShare extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, updatable = false, nullable = false)
    String id;

    @Column(name = "order_id", length = 36, nullable = false)
    String orderId;

    @Column(name = "course_id", length = 36, nullable = false)
    String courseId;

    @Column(name = "instructor_id", length = 36, nullable = false)
    String instructorId;

    /** Giá bán thực tế của khóa học trong đơn */
    @Column(name = "gross_amount", nullable = false)
    Long grossAmount;

    @Column(name = "currency_code", length = 10, nullable = false)
    String currencyCode;

    /** Snapshot tỷ lệ hoa hồng lúc chia — VD: 0.7000 = 70% cho Instructor */
    @Column(name = "commission_rate", precision = 5, scale = 4, nullable = false)
    BigDecimal commissionRate;

    /** gross_amount * commission_rate — phần Instructor nhận */
    @Column(name = "instructor_amount", nullable = false)
    Long instructorAmount;

    /** gross_amount - instructor_amount — phần Platform giữ lại */
    @Column(name = "platform_fee", nullable = false)
    Long platformFee;

    @Column(name = "is_deleted", nullable = false)
    Boolean deleted;

    @PrePersist
    public void prePersist() {
        if (this.currencyCode == null) this.currencyCode = "VND";
        if (this.deleted == null) this.deleted = false;
    }
}
