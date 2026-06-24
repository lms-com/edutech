package com.lms.order.model;


import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "order_details")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    Order order;

    @Column(name = "course_id", length = 36, nullable = false)
    String courseId;

    @Column(name = "course_name", length = 255, nullable = false)
    String courseName;

    @Column(name = "instructor_id", length = 36, nullable = false)
    String instructorId;

    @Column(name = "promotion_id", length = 36, nullable = true)
    String promotionId;

    @Column(name = "original_price", precision = 15, scale = 2, nullable = false)
    BigDecimal originalPrice;

    @Column(name = "original_currency", length = 3, nullable = false)
    String originalCurrency;

    @Column(name = "exchange_rate", precision = 12, scale = 6, nullable = false)
    @Builder.Default
    BigDecimal exchangeRate = BigDecimal.ONE;

    @Column(name = "price_at_purchase", precision = 15, scale = 2, nullable = false)
    BigDecimal priceAtPurchase;

    @Column(name = "discount_amount", precision = 15, scale = 2, nullable = false)
    @Builder.Default
    BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "final_price", precision = 15, scale = 2, nullable = false)
    BigDecimal finalPrice;
}
