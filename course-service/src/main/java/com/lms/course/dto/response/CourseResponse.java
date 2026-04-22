package com.lms.course.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class CourseResponse {
    private String id;
    private String title;
    private String slug;
    private String description;
    private String thumbnailUrl;
    private String level;
    private String instructorId;
    private String categoryId;
    private String categoryName;
    private BigDecimal basePrice;
    private String currencyCode;
    private String status;
}