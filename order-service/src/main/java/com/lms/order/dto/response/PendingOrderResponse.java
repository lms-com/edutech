package com.lms.order.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
public class PendingOrderResponse {
    String orderId;
    BigDecimal amount;
}
