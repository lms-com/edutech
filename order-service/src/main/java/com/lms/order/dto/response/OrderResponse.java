package com.lms.order.dto.response;

import com.lms.order.model.OrderStatus;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderResponse {
    String id;

    String learnerId;

    BigDecimal totalPrice;

    String currencyCode;

    OrderStatus status;

    String transactionNo;

    java.time.Instant paidAt;

    java.time.Instant createdAt;

    List<OrderDetailResponse> orderDetails;
}
