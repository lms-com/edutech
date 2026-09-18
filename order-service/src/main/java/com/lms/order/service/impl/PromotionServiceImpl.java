package com.lms.order.service.impl;

import com.lms.common.exception.AppException;
import com.lms.order.exception.OrderErrorCode;
import com.lms.order.model.Promotion;
import com.lms.order.repository.CoursePromotionRepository;
import com.lms.order.repository.PromotionRepository;
import com.lms.order.service.ExchangeRateService;
import com.lms.order.service.PromotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionServiceImpl implements PromotionService {

    private final PromotionRepository promotionRepository;
    private final CoursePromotionRepository coursePromotionRepository;
    private final ExchangeRateService exchangeRateService;

    @Override
    public Promotion getPromotionByCode(String promotionCode) {
        return promotionRepository.findByCode(promotionCode)
                .orElseThrow(() -> new AppException(OrderErrorCode.PROMOTION_NOT_FOUND, "Promotion " + promotionCode + " hasn't existed"));
    }

    @Override
    public void validateIsActive (Promotion promotion) {
        if (!promotion.isActive())
            throw new AppException(OrderErrorCode.PROMOTION_INACTIVE, "Promotion " + promotion.getCode() +" inactive");
    }

    @Override
    public void validateValidityPeriod(Promotion promotion) {
        if (promotion.getStartDate() != null && promotion.getStartDate().isAfter(LocalDateTime.now()))
            throw new AppException(OrderErrorCode.PROMOTION_NOT_STARTED_YET);
        if (promotion.getEndDate() != null && promotion.getEndDate().isBefore(LocalDateTime.now()))
            throw new AppException(OrderErrorCode.PROMOTION_EXPIRED);
    }

    @Override
    public void validateUsageLimit (Promotion promotion) {
        if (promotion.getUsageLimit() == null)
            return;
        if (promotion.getUsageCount().compareTo(promotion.getUsageLimit()) >= 0)
            throw new AppException(OrderErrorCode.PROMOTION_LIMIT_USAGE_EXCEEDED, "Promotion " + promotion.getCode() + " limit exceeded");
    }

    @Override
    public boolean isValidForCourse(String courseId, String promotionId) {
        return coursePromotionRepository.existsByPromotionIdAndCourseId(promotionId, courseId);
    }

    @Override
    @Transactional
    public void increaseUsageCountBatch (List<String> promotionIds) {
        Map<String, Integer> promoMapCount = promotionIds.stream()
                .collect(Collectors.toMap(item -> item, item -> 1,  Integer::sum));

        List<Promotion> promotionList = promotionRepository.findAllByIds(promotionIds);
        if (promotionList.isEmpty()) log.warn("⚠️No promotion found with ids {}", promotionIds);
        else log.info("🆗 Promotions were found!");

        promotionList.forEach(promotion -> {
            int currentUsage = (promotion.getUsageCount() == null) ? 0 : promotion.getUsageCount();
            int additionUsage = promoMapCount.getOrDefault(promotion.getId(), 0);
            promotion.setUsageCount(
                    currentUsage + additionUsage
            );
        });
        promotionRepository.saveAll(promotionList);
    }

    @Override
    public BigDecimal calculateDiscountAmount(BigDecimal originalPrice, Promotion promotion) {
        return promotion.getDiscountPercent() != null ?
                (originalPrice.multiply(promotion.getDiscountPercent())).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : promotion.getDiscountAmount();
    }
}