package com.lms.order.service.impl;

import com.lms.common.exception.AppException;
import com.lms.order.dto.request.CreatePromotionRequest;
import com.lms.order.dto.response.PromotionResponse;
import com.lms.order.dto.response.PromotionValidateResponse;
import com.lms.order.exception.OrderErrorCode;
import com.lms.order.model.CoursePromotion;
import com.lms.order.model.Promotion;
import com.lms.order.repository.CoursePromotionRepository;
import com.lms.order.repository.PromotionRepository;
import com.lms.order.service.ExchangeRateService;
import com.lms.order.service.PromotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    // =========================================================
    // ========= CÁC METHOD NỘI BỘ (dùng trong createOrder) ===
    // =========================================================

    @Override
    public Promotion getPromotionByCode(String promotionCode) {
        return promotionRepository.findByCode(promotionCode)
                .orElseThrow(() -> new AppException(OrderErrorCode.PROMOTION_NOT_FOUND, "Promotion " + promotionCode + " hasn't existed"));
    }

    @Override
    public void validateIsActive(Promotion promotion) {
        if (!promotion.isActive())
            throw new AppException(OrderErrorCode.PROMOTION_INACTIVE, "Promotion " + promotion.getCode() + " inactive");
    }

    @Override
    public void validateValidityPeriod(Promotion promotion) {
        if (promotion.getStartDate() != null && promotion.getStartDate().isAfter(LocalDateTime.now()))
            throw new AppException(OrderErrorCode.PROMOTION_NOT_STARTED_YET);
        if (promotion.getEndDate() != null && promotion.getEndDate().isBefore(LocalDateTime.now()))
            throw new AppException(OrderErrorCode.PROMOTION_EXPIRED);
    }

    @Override
    public void validateUsageLimit(Promotion promotion) {
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
    public void increaseUsageCountBatch(List<String> promotionIds) {
        if (promotionIds == null || promotionIds.isEmpty()) {
            return;
        }

        Map<String, Integer> promoMapCount = promotionIds.stream()
                .collect(Collectors.toMap(item -> item, item -> 1, Integer::sum));

        List<Promotion> promotionList = promotionRepository.findAllByIds(promotionIds);
        if (promotionList.isEmpty()) log.warn("⚠️No promotion found with ids {}", promotionIds);
        else log.info("🆗 Promotions were found!");

        promotionList.forEach(promotion -> {
            int currentUsage = (promotion.getUsageCount() == null) ? 0 : promotion.getUsageCount();
            int additionUsage = promoMapCount.getOrDefault(promotion.getId(), 0);
            promotion.setUsageCount(currentUsage + additionUsage);
        });
        promotionRepository.saveAll(promotionList);
    }

    @Override
    public BigDecimal calculateDiscountAmount(BigDecimal originalPrice, Promotion promotion) {
        return promotion.getDiscountPercent() != null
                ? (originalPrice.multiply(promotion.getDiscountPercent())).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : promotion.getDiscountAmount();
    }

    // =========================================================
    // ========= PRODUCTION API: CRUD MÃ GIẢM GIÁ =============
    // =========================================================

    @Override
    @Transactional
    public PromotionResponse createPromotion(CreatePromotionRequest request, String creatorId) {
        // Kiểm tra trùng mã (không phân biệt hoa thường)
        String upperCode = request.getCode().toUpperCase();
        if (promotionRepository.findByCode(upperCode).isPresent()) {
            throw new AppException(OrderErrorCode.PROMOTION_NOT_FOUND,
                    "Mã giảm giá '" + upperCode + "' đã tồn tại trong hệ thống. Vui lòng chọn mã khác.");
        }

        // Bắt buộc phải có ít nhất 1 trong 2: % hoặc số tiền
        if (request.getDiscountPercent() == null && request.getDiscountAmount() == null) {
            throw new AppException(OrderErrorCode.PROMOTION_NOT_APPLICABLE,
                    "Vui lòng nhập phần trăm hoặc số tiền giảm giá.");
        }

        Promotion promotion = Promotion.builder()
                .code(upperCode)
                .discountPercent(request.getDiscountPercent())
                .discountAmount(request.getDiscountAmount())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .usageLimit(request.getUsageLimit())
                .usageCount(0)
                .isActive(true)
                .build();

        Promotion saved = promotionRepository.save(promotion);
        log.info("✅ Đã tạo mã giảm giá [{}] bởi người dùng [{}]", upperCode, creatorId);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public PromotionResponse updatePromotion(String promotionId, CreatePromotionRequest request, String updaterId) {
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new AppException(OrderErrorCode.PROMOTION_NOT_FOUND,
                        "Không tìm thấy mã giảm giá với id: " + promotionId));

        // Nếu đổi mã code, kiểm tra trùng
        String upperCode = request.getCode().toUpperCase();
        if (!promotion.getCode().equals(upperCode)) {
            if (promotionRepository.findByCode(upperCode).isPresent()) {
                throw new AppException(OrderErrorCode.PROMOTION_NOT_APPLICABLE,
                        "Mã giảm giá '" + upperCode + "' đã tồn tại. Vui lòng chọn mã khác.");
            }
            promotion.setCode(upperCode);
        }

        promotion.setDiscountPercent(request.getDiscountPercent());
        promotion.setDiscountAmount(request.getDiscountAmount());
        promotion.setStartDate(request.getStartDate());
        promotion.setEndDate(request.getEndDate());
        promotion.setUsageLimit(request.getUsageLimit());

        Promotion saved = promotionRepository.save(promotion);
        log.info("✏️ Đã cập nhật mã giảm giá [{}] bởi người dùng [{}]", upperCode, updaterId);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void deletePromotion(String promotionId, String userId) {
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new AppException(OrderErrorCode.PROMOTION_NOT_FOUND,
                        "Không tìm thấy mã giảm giá với id: " + promotionId));
        // Xóa tất cả liên kết course-promotion trước
        List<CoursePromotion> links = coursePromotionRepository.findByPromotionId(promotionId);
        coursePromotionRepository.deleteAll(links);
        promotionRepository.delete(promotion);
        log.info("🗑️ Đã xóa mã giảm giá [{}] bởi người dùng [{}]", promotion.getCode(), userId);
    }

    @Override
    public Page<PromotionResponse> getMyPromotions(String instructorId, Pageable pageable) {
        return promotionRepository.findByCreatedBy(instructorId, pageable)
                .map(this::toResponse);
    }

    @Override
    public Page<PromotionResponse> getAllPromotions(Pageable pageable) {
        return promotionRepository.findAll(pageable)
                .map(this::toResponse);
    }

    // =========================================================
    // ========= GẮN / GỠ MÃ KHỎI KHÓA HỌC ====================
    // =========================================================

    @Override
    @Transactional
    public PromotionResponse attachToAnyCourse(String promotionId, String courseId, String userId) {
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new AppException(OrderErrorCode.PROMOTION_NOT_FOUND,
                        "Không tìm thấy mã giảm giá với id: " + promotionId));

        // Tránh gắn trùng
        if (coursePromotionRepository.existsByPromotionIdAndCourseId(promotionId, courseId)) {
            log.info("ℹ️ Mã [{}] đã được gắn vào khóa học [{}] từ trước.", promotion.getCode(), courseId);
            return toResponse(promotion);
        }

        coursePromotionRepository.save(
                CoursePromotion.builder()
                        .promotion(promotion)
                        .courseId(courseId)
                        .build()
        );
        log.info("🔗 Đã gắn mã [{}] vào khóa học [{}] bởi người dùng [{}]", promotion.getCode(), courseId, userId);
        return toResponse(promotion);
    }

    @Override
    @Transactional
    public PromotionResponse detachFromCourse(String promotionId, String courseId, String userId) {
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new AppException(OrderErrorCode.PROMOTION_NOT_FOUND,
                        "Không tìm thấy mã giảm giá với id: " + promotionId));

        coursePromotionRepository.deleteByPromotionIdAndCourseId(promotionId, courseId);
        log.info("🔓 Đã gỡ mã [{}] khỏi khóa học [{}] bởi người dùng [{}]", promotion.getCode(), courseId, userId);
        return toResponse(promotion);
    }

    // =========================================================
    // ========= HỌC VIÊN KIỂM TRA MÃ TRƯỚC KHI ĐẶT ===========
    // =========================================================

    @Override
    public PromotionValidateResponse validateCode(String code, String courseId, BigDecimal coursePrice) {
        try {
            Promotion promotion = getPromotionByCode(code.toUpperCase());
            validateIsActive(promotion);
            validateValidityPeriod(promotion);
            validateUsageLimit(promotion);

            boolean validForCourse = isValidForCourse(courseId, promotion.getId());
            if (!validForCourse) {
                return PromotionValidateResponse.builder()
                        .valid(false)
                        .code(code)
                        .message("Mã giảm giá này không áp dụng cho khóa học đã chọn.")
                        .build();
            }

            BigDecimal discountAmt = calculateDiscountAmount(coursePrice, promotion);
            if (discountAmt.compareTo(coursePrice) > 0) discountAmt = coursePrice;
            BigDecimal finalPrice = coursePrice.subtract(discountAmt);

            return PromotionValidateResponse.builder()
                    .valid(true)
                    .code(promotion.getCode())
                    .message("Mã giảm giá hợp lệ! Bạn được giảm " +
                            (promotion.getDiscountPercent() != null
                                    ? promotion.getDiscountPercent() + "%"
                                    : promotion.getDiscountAmount() + "đ"))
                    .discountPercent(promotion.getDiscountPercent())
                    .discountAmount(discountAmt)
                    .originalPrice(coursePrice)
                    .finalPrice(finalPrice)
                    .build();

        } catch (AppException e) {
            return PromotionValidateResponse.builder()
                    .valid(false)
                    .code(code)
                    .message(e.getMessage())
                    .build();
        }
    }

    // =========================================================
    // ========= HELPER: Chuyển entity -> DTO ==================
    // =========================================================

    private PromotionResponse toResponse(Promotion p) {
        List<String> courseIds = coursePromotionRepository.findByPromotionId(p.getId())
                .stream().map(CoursePromotion::getCourseId).toList();
        return PromotionResponse.builder()
                .id(p.getId())
                .code(p.getCode())
                .discountPercent(p.getDiscountPercent())
                .discountAmount(p.getDiscountAmount())
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .usageLimit(p.getUsageLimit())
                .usageCount(p.getUsageCount())
                .isActive(p.isActive())
                .appliedCourseIds(courseIds)
                .build();
    }
}