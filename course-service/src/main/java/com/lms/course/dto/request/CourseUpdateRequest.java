package com.lms.course.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CourseUpdateRequest {
    // Trong PATCH/Partial Update, tất cả các trường đều có thể null
    // (Bỏ qua @NotBlank, @NotNull)
    private String title;
    private String slug;
    private String categoryId;
    private String description;
    private String thumbnailUrl;
    private String level;
    private BigDecimal basePrice;
    private String currencyCode;
}