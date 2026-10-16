package com.lms.order.service;

import com.lms.order.dto.request.CreatePromotionRequest;
import com.lms.order.dto.response.PromotionResponse;
import com.lms.order.dto.response.PromotionValidateResponse;
import com.lms.order.model.Promotion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface PromotionService {

    // ===== Các method nội bộ dùng trong createOrder =====
    Promotion getPromotionByCode (String promotionCode);
    void validateIsActive (Promotion promotion);
    void validateValidityPeriod (Promotion promotion);
    void validateUsageLimit (Promotion promotion);
    boolean isValidForCourse (String courseId, String promotionId);
    void increaseUsageCountBatch (List<String> promotionIds);
    BigDecimal calculateDiscountAmount (BigDecimal originalPrice, Promotion promotion);

    // ===== Production API: Admin/Giảng viên quản lý mã =====
    PromotionResponse createPromotion(CreatePromotionRequest request, String creatorId);
    PromotionResponse updatePromotion(String promotionId, CreatePromotionRequest request, String updaterId);
    void deletePromotion(String promotionId, String userId);
    Page<PromotionResponse> getMyPromotions(String instructorId, Pageable pageable);
    Page<PromotionResponse> getAllPromotions(Pageable pageable);   // Admin only

    // ===== Gắn / gỡ mã khỏi khóa học =====
    PromotionResponse attachToAnyCourse(String promotionId, String courseId, String userId);
    PromotionResponse detachFromCourse(String promotionId, String courseId, String userId);

    // ===== Học viên kiểm tra mã trước khi đặt hàng =====
    PromotionValidateResponse validateCode(String code, String courseId, BigDecimal coursePrice);
}
