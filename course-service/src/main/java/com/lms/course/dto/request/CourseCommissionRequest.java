package com.lms.course.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CourseCommissionRequest {
    @NotNull(message = "Tỷ lệ hoa hồng không được để trống")
    private BigDecimal overrideCommissionRate;
}
