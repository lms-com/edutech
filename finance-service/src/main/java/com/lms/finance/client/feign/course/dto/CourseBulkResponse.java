package com.lms.finance.client.feign.course.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CourseBulkResponse {
    String id;
    String title;
    BigDecimal basePrice;
    String instructorId;
    BigDecimal commissionRate;
}
