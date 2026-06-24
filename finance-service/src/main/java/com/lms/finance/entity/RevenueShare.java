package com.lms.finance.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "revenue_shares",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_revenue_order_course",
                        columnNames = {"order_id", "course_id"})
        },
        indexes = {
                @Index(name = "idx_revenue_instructor", columnList = "instructor_id, created_at DESC"),
                @Index(name = "idx_revenue_course_id", columnList = "course_id"),
                @Index(name = "idx_revenue_created_at", columnList = "created_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@EntityListeners(AuditingEntityListener.class)
public class RevenueShare {

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
    @Column(name = "gross_amount", precision = 15, scale = 2, nullable = false)
    Long grossAmount;

    @Column(name = "currency_code", length = 3, nullable = false)
    @Builder.Default
    String currencyCode = "VND";

    /** Snapshot tỷ lệ hoa hồng lúc chia — VD: 0.7000 = 70% cho Instructor */
    @Column(name = "commission_rate", precision = 5, scale = 4, nullable = false)
    BigDecimal commissionRate;

    /** gross_amount * commission_rate — phần Instructor nhận */
    @Column(name = "instructor_amount", precision = 15, scale = 2, nullable = false)
    BigDecimal instructorAmount;

    /** gross_amount - instructor_amount — phần Platform giữ lại */
    @Column(name = "platform_fee", precision = 15, scale = 2, nullable = false)
    BigDecimal platformFee;

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    Instant createdAt;

}
