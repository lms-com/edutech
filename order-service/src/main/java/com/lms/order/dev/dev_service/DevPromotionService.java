package com.lms.order.dev.dev_service;

import com.lms.order.dev.dev_dto.CoursePromotionResponse;
import com.lms.order.dto.request.CoursePromotionRequest;
import com.lms.order.model.CoursePromotion;
import com.lms.order.model.Order;
import com.lms.order.model.Promotion;
import com.lms.order.repository.CoursePromotionRepository;
import com.lms.order.repository.PromotionRepository;
import com.lms.order.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DevPromotionService {
    private final PromotionRepository promotionRepository;
    private final PromotionService promotionService;
    private final CoursePromotionRepository coursePromotionRepository;

    /**
     * Get all promotion objects in db
     * @return List of Promotion
     */
    public List<Promotion> getAllPromotions(){
        return promotionRepository.findAll();
    }

    /**
     * Set promotion for course
     * @param promotionCode
     * @param courseId
     * @return CoursePromotion
     */
    public CoursePromotion setPromotionForCourse(CoursePromotionRequest req) {
        Promotion promotion = promotionService.getPromotionByCode(req.getPromotionCode());
        return coursePromotionRepository.save(CoursePromotion.builder()
                .promotion(promotion)
                .courseId(req.getCourseId())
                .build()
        );
    }

    /**
     * Get all Course-Promotion
     * @return List of CoursePromotion
     */
    public List<CoursePromotionResponse> getAllCoursePromotions(){
        return coursePromotionRepository.findAll().stream().map(cp -> CoursePromotionResponse.builder()
                .id(cp.getId())
                .promotion(CoursePromotionResponse.PromotionResponse.builder()
                        .id(cp.getPromotion().getId())
                        .code(cp.getPromotion().getCode())
                        .discountPercent(cp.getPromotion().getDiscountPercent())
                        .discountAmount(cp.getPromotion().getDiscountAmount())
                        .isActive(cp.getPromotion().isActive())
                        .build())
                .courseId(cp.getCourseId())
                .build()
        ).toList();
    }
}
