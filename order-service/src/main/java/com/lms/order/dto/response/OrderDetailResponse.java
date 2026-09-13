package com.lms.order.dto.response;


import jakarta.persistence.Column;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderDetailResponse {

    String courseName;

    BigDecimal originalPrice;

    String originalCurrency;

    BigDecimal exchangeRate;

    BigDecimal priceAtPurchase;

    BigDecimal discountAmount;

    BigDecimal finalPrice;
}
