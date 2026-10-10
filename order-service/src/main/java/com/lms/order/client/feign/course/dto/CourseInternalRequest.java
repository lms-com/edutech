package com.lms.order.client.feign.course.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CourseInternalRequest {
    @JsonProperty("id")
    String courseId;

    @JsonProperty("title")
    String courseName;

    @JsonProperty("basePrice")
    BigDecimal currentPrice;

    @JsonProperty("instructorId")
    String instructorId;

    @JsonProperty("commissionRate")
    BigDecimal commissionRate;

}