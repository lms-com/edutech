package com.lms.analytics.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CoursePerformanceDto {
    String courseId;
    BigDecimal revenue;
    Long enrollments;
    String courseName;   // Sẽ nhặt từ Redis
    String thumbnailUrl; // Sẽ nhặt từ Redis

    // Constructor dùng riêng cho JPA Query (chỉ map những trường DB Analytics có)
    public CoursePerformanceDto(String courseId, BigDecimal revenue, Long enrollments) {
        this.courseId = courseId;
        this.revenue = revenue;
        this.enrollments = enrollments;
    }
}