package com.lms.finance.client.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CourseInternalRequest {
    @JsonProperty("id")
    String courseId;

    @JsonProperty("paymentCurrency")
    String currencyCode;

    @JsonProperty("instructorId")
    String instructorId;

    @JsonProperty("commissionRate")
    BigDecimal commissionRate;

    @JsonProperty("finalPrice")
    BigDecimal priceAtPurchase;
}
