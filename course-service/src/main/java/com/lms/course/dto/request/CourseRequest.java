package com.lms.course.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CourseRequest {
    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;

    @NotBlank(message = "Slug không được để trống")
    private String slug;

    @NotBlank(message = "ID Danh mục không được để trống")
    private String categoryId;

    private String description;
    private String thumbnailUrl;
    private String level;

    @NotNull(message = "Giá gốc không được để trống")
    private BigDecimal basePrice;

    private String currencyCode;
}