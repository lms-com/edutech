package com.lms.order.dto.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CoursePromotionRequest {
    private String promotionCode;
    private String courseId;
}
