package com.lms.order.model;

import com.lms.common.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "promotions")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Promotion extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(name = "code", length = 20, unique = true, nullable = false)
    String code;

    @Column(name = "discount_percent", precision = 5, scale = 2)
    BigDecimal discountPercent;

    @Column(name = "discount_amount", precision = 15, scale = 2)
    BigDecimal discountAmount;

    @Column(name = "start_date")
    LocalDateTime startDate;

    @Column(name = "end_date")
    LocalDateTime endDate;

    @Column(name = "usage_limit")
    Integer usageLimit;

    @Column(name = "usage_count")
    @Builder.Default
    Integer usageCount = 0;

    @Column(name = "is_active")
    @Builder.Default
    boolean isActive = true;
}
