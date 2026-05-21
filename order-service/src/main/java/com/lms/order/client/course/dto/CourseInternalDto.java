package com.lms.order.client.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CourseInternalDto {
    @JsonProperty("id")
    String courseId;

    @JsonProperty("title")
    String courseName;

    @JsonProperty("basePrice")
    BigDecimal currentPrice;

    @JsonProperty("currencyCode")
    String currencyCode;

    @JsonProperty("instructorId")
    String instructorId;
}