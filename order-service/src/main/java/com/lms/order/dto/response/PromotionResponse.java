package com.lms.order.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PromotionResponse {

    String id;
    String code;
    BigDecimal discountPercent;
    BigDecimal discountAmount;
    LocalDateTime startDate;
    LocalDateTime endDate;
    Integer usageLimit;
    Integer usageCount;
    boolean isActive;
    // Danh sách courseId được áp dụng (nếu có)
    java.util.List<String> appliedCourseIds;
}
