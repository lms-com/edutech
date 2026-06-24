package com.lms.order.service;

import com.lms.order.model.Promotion;

import java.math.BigDecimal;

public interface PromotionService {

    Promotion getPromotionByCode (String promotionCode);
    void validateIsActive (Promotion promotion);
    void validateValidityPeriod (Promotion promotion);
    void validateUsageLimit (Promotion promotion);
    boolean isValidForCourse (String courseId, String promotionId);
    void increaseUsageCount (Promotion promotion);
    BigDecimal calculateDiscountAmount (BigDecimal originalPrice, String currencyCode, Promotion promotion);
}
