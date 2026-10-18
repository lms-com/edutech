package com.lms.course.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class CourseDetailResponse {
    // Thông tin cơ bản của khóa học (giống CourseResponse)
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
    private String rejectionNote;
    private BigDecimal overrideCommissionRate;

    // Cấu trúc Chương trình học (Curriculum) lồng nhau: Course → Sections → Lessons
    private List<SectionResponse> sections;
}