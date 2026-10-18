package com.lms.order.service;

import com.lms.order.model.Promotion;

import java.math.BigDecimal;
import java.util.List;

public interface PromotionService {

    Promotion getPromotionByCode (String promotionCode);
    void validateIsActive (Promotion promotion);
    void validateValidityPeriod (Promotion promotion);
    void validateUsageLimit (Promotion promotion);
    boolean isValidForCourse (String courseId, String promotionId);

    void increaseUsageCountBatch (List<String> promotionIds);

    BigDecimal calculateDiscountAmount (BigDecimal originalPrice, Promotion promotion);
}
