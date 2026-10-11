package com.lms.analytics.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyChartDto {
    private LocalDate date;
    private BigDecimal revenue;
    private Long enrollments; // Count/Sum trong JPQL thường trả về Long
}
