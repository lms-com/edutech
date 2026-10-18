package com.lms.order.dev.dev_dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CoursePromotionResponse {
    private String id;
    private PromotionResponse promotion;
    private String courseId;

    @Data
    @Builder
    public static class PromotionResponse {
        private String id;
        private String code;
        private BigDecimal discountPercent;
        private BigDecimal discountAmount;
        private boolean isActive;

    }
}
