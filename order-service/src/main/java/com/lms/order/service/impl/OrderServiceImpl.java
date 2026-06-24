package com.lms.order.service.impl;

import com.lms.order.client.course.CourseServiceFeignClient;
import com.lms.order.client.course.dto.CourseInternalDto;
import com.lms.order.dto.request.CreateOrderRequest;
import com.lms.order.dto.response.OrderResponse;
import com.lms.order.mapper.OrderMapper;
import com.lms.order.model.Order;
import com.lms.order.model.OrderDetail;
import com.lms.order.model.OrderStatus;
import com.lms.order.model.Promotion;
import com.lms.order.repository.OrderRepository;
import com.lms.order.service.ExchangeRateService;
import com.lms.order.service.OrderService;
import com.lms.order.service.PromotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CourseServiceFeignClient courseClient;
    private final PromotionService promotionService;
    private final ExchangeRateService exchangeRateService;
    private final OrderMapper orderMapper;


    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, String learnerId) {
        // Tao doi tuong Order
        Order order = Order.builder()
                .learnerId(learnerId)
                .currencyCode(request.getCurrencyCode())
                .status(OrderStatus.PENDING)
                .build();

        // Goi lay thong tin khoa hoc ben Course
        List<String> listCourseIds = request.getItems().stream()
                        .map(CreateOrderRequest.CartItemRequest::getCourseId)
                        .toList();
        List<CourseInternalDto> listCourseInfos = courseClient.getCoursesById(listCourseIds);

        // Tao Map de lay promotionCode theo courseId
        Map<String, String> coursePromotionCodeMap = request.getItems().stream()
                .collect(Collectors.toMap(
                        CreateOrderRequest.CartItemRequest::getCourseId,
                        item -> item.getPromotionCode() != null ? item.getPromotionCode() : "",  // Tra ve "" thay vi null tranh loi NullPointerException
                        (existing, replacement) -> replacement      // neu key (courseId) trung lap thi nhan value sau (replacement)
                ));
        // Tao bien luu tong tien vao Order
        BigDecimal totalPrice = BigDecimal.ZERO;

        // Tao tung OrderDetail tuong ung voi tung Course Info
        for (CourseInternalDto courseInfo : listCourseInfos) {
            // Kiem tra va lay promotion truoc khi tinh tien
            String promotionCode = coursePromotionCodeMap.getOrDefault(courseInfo.getCourseId(), "");
            Promotion promotion = null;
            BigDecimal discountAmount = BigDecimal.ZERO;

            // Neu khac tien te goc thi doi tien truoc
            BigDecimal exchangeRate = exchangeRateService.getRate(courseInfo.getCurrencyCode(), request.getCurrencyCode());
            BigDecimal priceAtPurchase = exchangeRate.multiply(courseInfo.getCurrentPrice());

            // Neu request co promotion kem voi course
            if (!promotionCode.trim().isEmpty()) {
                // Lay doi tuong Promotion tu code
                promotion = promotionService.getPromotionByCode(promotionCode);
                // Kiem tra xac thuc promotion
                promotionService.validateIsActive(promotion);       // Kiem tra trang thai active
                promotionService.validateValidityPeriod(promotion); // Kiem tra thoi gian hieu luc
                promotionService.validateUsageLimit(promotion);     // Kiem tra gioi han so luong su dung

                boolean isValid = promotionService.isValidForCourse(courseInfo.getCourseId(), promotion.getId());
                // Neu promotion ko ap dung cho course thi gan null cho promotion do
                if (!isValid) {
                    log.warn("❌ Promotion code {} is invalid for course {}", promotionCode, courseInfo.getCourseName());
                    promotion = null;
                } else {
                    // Promotion hop le thi tinh so tien duoc giam va tang usage count
                    log.info("✅ Promotion code {} is valid", promotionCode);
                    discountAmount = promotionService.calculateDiscountAmount(priceAtPurchase, request.getCurrencyCode(), promotion);
                    // Chan truong hop tien giam nhieu hon tien goc
                    if (discountAmount.compareTo(priceAtPurchase) > 0) {
                        discountAmount = priceAtPurchase;
                    }
                    promotionService.increaseUsageCount(promotion);
                }
            }

            // Tinh gia tien cuoi cung cua khoa hoc
            BigDecimal finalPrice = priceAtPurchase.subtract(discountAmount);
            totalPrice = totalPrice.add(finalPrice);

            // Tao va gan OrderDetail moi vao Order
            order.addOrderDetail(OrderDetail.builder()
                            .courseId(courseInfo.getCourseId())
                            .courseName(courseInfo.getCourseName())
                            .instructorId(courseInfo.getInstructorId())
                            .promotionId(promotion == null ? null : promotion.getId())
                            .originalPrice(courseInfo.getCurrentPrice())
                            .originalCurrency(courseInfo.getCurrencyCode())
                            .exchangeRate(exchangeRate)
                            .priceAtPurchase(priceAtPurchase)
                            .discountAmount(discountAmount)
                            .finalPrice(finalPrice)
                            .build());
        }
        order.setTotalPrice(totalPrice);
        return orderMapper.toOrderResponse(orderRepository.save(order));
    }
}