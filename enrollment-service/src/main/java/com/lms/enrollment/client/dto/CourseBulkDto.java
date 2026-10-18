package com.lms.enrollment.client.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CourseBulkDto {
    private String id;
    private String title;
    private BigDecimal basePrice;
    private String instructorId;
    private BigDecimal commissionRate;
}
