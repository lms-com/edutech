package com.lms.enrollment.client.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CoursePriceInfoDto {
    private String courseId;
    private BigDecimal basePrice;
    private String status;
}
