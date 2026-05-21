package com.lms.order.model;

import com.lms.common.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "orders")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Order extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(name = "learner_id", length = 36, nullable = false)
    String learnerId;

    @Column(name = "total_price", precision = 15, scale = 2, nullable = false)
    @Builder.Default
    BigDecimal totalPrice = BigDecimal.ZERO;

    @Column(name = "currency_code", length = 3, nullable = false)
    @Builder.Default
    String currencyCode = "VND";

    @Column(name = "exchange_rate", precision = 12, scale = 6, nullable = false)
    BigDecimal exchangeRate = BigDecimal.ONE;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    OrderStatus status;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<OrderDetail> orderDetails = new ArrayList<>();


    public void addOrderDetail(OrderDetail orderDetail) {
        orderDetails.add(orderDetail);
        orderDetail.setOrder(this);
    }
}
