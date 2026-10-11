package com.lms.analytics.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InstructorPerformanceDto {
    String instructorId;
    BigDecimal totalRevenue;
    Long totalEnrollments;

    String instructorName; // Có thể lấy thêm sau nếu cần
    String avatarUrl;      // Có thể lấy thêm sau nếu cần

    // Constructor dùng cho JPA Query
    public InstructorPerformanceDto(String instructorId, BigDecimal totalRevenue, Long totalEnrollments) {
        this.instructorId = instructorId;
        this.totalRevenue = totalRevenue;
        this.totalEnrollments = totalEnrollments;
    }
}
