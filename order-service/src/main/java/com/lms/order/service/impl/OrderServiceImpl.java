package com.lms.order.service.impl;

import com.lms.common.dto.response.ApiResponse;
import com.lms.common.exception.AppException;
import com.lms.order.client.feign.course.CourseServiceFeignClient;
import com.lms.order.client.feign.course.dto.CourseInternalRequest;
import com.lms.order.client.feign.enrollment.EnrollmentFeignClient;
import com.lms.order.client.feign.enrollment.dto.EnrollmentValidationResponse;
import com.lms.order.client.feign.finance.FinanceServiceFeignClient;
import com.lms.order.client.feign.finance.dto.CreatePaymentDto;
import com.lms.order.dto.message.OrderCompletedMessage;
import com.lms.order.dto.request.CreateOrderRequest;
import com.lms.order.dto.response.PendingOrderResponse;
import com.lms.order.exception.OrderErrorCode;
import com.lms.order.model.Order;
import com.lms.order.model.OrderDetail;
import com.lms.order.model.OrderStatus;
import com.lms.order.model.Promotion;
import com.lms.order.repository.OrderRepository;
import com.lms.order.service.OrderService;
import com.lms.order.service.PromotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lms.order.dto.response.OrderResponse;
import com.lms.order.mapper.OrderMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.lms.order.config.RabbitMQConfig.ORDER_EXCHANGE;
import static com.lms.order.config.RabbitMQConfig.ORDER_COMPLETED_ROUTING_KEY;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final CourseServiceFeignClient courseClient;
    private final PromotionService promotionService;
    private final FinanceServiceFeignClient financeClient;
    private final EnrollmentFeignClient enrollmentClient;
    private final RabbitTemplate rabbitTemplate;
    private final String DEFAULT_CURRENCY = "VND";


    @Override
    @Transactional
    public String createOrderAndGetPaymentUrl(CreateOrderRequest request, String userId) {
        PendingOrderResponse orderInfo = createOrder(request, userId);
        long longAmount = orderInfo.getAmount().longValue();
        try {
            log.info("🤙 Calling finance-service for orderId: {}", orderInfo.getOrderId());
            return financeClient.getPaymentUrl(
                    CreatePaymentDto.builder()
                            .learnerId(userId)
                            .orderId(orderInfo.getOrderId())
                            .paymentMethod(request.getPaymentMethod().toUpperCase())
                            .amount(longAmount)
                            .currencyCode(DEFAULT_CURRENCY)
                    .build());

        } catch (Exception e) {
            log.error("❎ Lỗi khi kết nối tới finance-service: ", e);
            throw new AppException(OrderErrorCode.PAYMENT_NOT_CONNECTED);
        }
    }

    private void validateNotAlreadyEnrolled(CreateOrderRequest request, String learnerId) {
        if (enrollmentClient == null || request.getItems() == null) return;
        for (CreateOrderRequest.CartItemRequest item : request.getItems()) {
            try {
                ApiResponse<EnrollmentValidationResponse> res = enrollmentClient.validateAccess(learnerId, item.getCourseId());
                if (res != null && res.getData() != null && Boolean.TRUE.equals(res.getData().getHasAccess())) {
                    log.warn("⚠️ Học viên {} đã sở hữu khóa học {}", learnerId, item.getCourseId());
                    throw new AppException(OrderErrorCode.COURSE_ALREADY_PURCHASED, "Bạn đã sở hữu khóa học này rồi");
                }
            } catch (AppException e) {
                throw e;
            } catch (Exception e) {
                log.warn("Không thể kiểm tra ghi danh từ enrollment-service: {}", e.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public PendingOrderResponse createOrder(CreateOrderRequest request, String learnerId) {
        // 1. Kiểm tra xem học viên đã sở hữu khóa học nào trong đơn chưa
        validateNotAlreadyEnrolled(request, learnerId);

        // 2. Kiểm tra các đơn PENDING hiện có của học viên
        Instant now = Instant.now();
        Instant fifteenMinutesAgo = now.minus(15, ChronoUnit.MINUTES);

        List<Order> existingPendingOrders = orderRepository.findByLearnerIdAndStatus(learnerId, OrderStatus.PENDING);
        Set<String> requestCourseIds = request.getItems().stream()
                .map(CreateOrderRequest.CartItemRequest::getCourseId)
                .collect(Collectors.toSet());

        for (Order pendingOrder : existingPendingOrders) {
            if (pendingOrder.getCreatedAt() != null && pendingOrder.getCreatedAt().isBefore(fifteenMinutesAgo)) {
                // Đơn đã quá 15 phút -> tự động hủy (CANCELLED)
                pendingOrder.setStatus(OrderStatus.CANCELLED);
                orderRepository.save(pendingOrder);
                log.info("⏰ Đơn hàng {} của học viên {} đã quá 15 phút, tự động chuyển sang CANCELLED", pendingOrder.getId(), learnerId);
            } else {
                // Đơn còn hiệu lực trong vòng 15 phút -> kiểm tra xem có cùng danh sách khóa học không
                Set<String> pendingCourseIds = pendingOrder.getOrderDetails().stream()
                        .map(OrderDetail::getCourseId)
                        .collect(Collectors.toSet());
                if (pendingCourseIds.equals(requestCourseIds)) {
                    log.info("♻️ Tái sử dụng đơn hàng PENDING còn hiệu lực ({}) cho học viên {}", pendingOrder.getId(), learnerId);
                    return new PendingOrderResponse(pendingOrder.getId(), pendingOrder.getTotalPrice());
                }
            }
        }

        // 3. Nếu không có đơn PENDING trùng khớp còn hiệu lực -> Tạo đơn mới
        Order order = Order.builder()
                .learnerId(learnerId)
                .currencyCode(DEFAULT_CURRENCY)
                .status(OrderStatus.PENDING)
                .build();

        // Goi lay thong tin khoa hoc ben Course
        List<String> listCourseIds = request.getItems().stream()
                        .map(CreateOrderRequest.CartItemRequest::getCourseId)
                        .toList();
        List<CourseInternalRequest> listCourseInfos = courseClient.getCoursesById(listCourseIds);

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
        for (CourseInternalRequest courseInfo : listCourseInfos) {
            // Kiem tra va lay promotion truoc khi tinh tien
            String promotionCode = coursePromotionCodeMap.getOrDefault(courseInfo.getCourseId(), "");
            Promotion promotion = null;
            BigDecimal discountAmount = BigDecimal.ZERO;
            BigDecimal coursePrice = courseInfo.getCurrentPrice();

            log.info("💵💵 CourseId {} has cost is {}", courseInfo.getCourseId(), coursePrice);
            log.info("📉 PromotionCode {} for course id {}", promotionCode, courseInfo.getCourseId());

            // Neu request co promotion kem voi course
            if (!promotionCode.trim().isEmpty()) {
                // Lay doi tuong Promotion tu code
                promotion = promotionService.getPromotionByCode(promotionCode);
                // Kiem tra xac thuc promotion
                promotionService.validateIsActive(promotion);       // Kiem tra trang thai active
                promotionService.validateValidityPeriod(promotion); // Kiem tra thoi gian hieu luc
                promotionService.validateUsageLimit(promotion);     // Kiem tra gioi han so luong su dung

                boolean isValid = promotionService.isValidForCourse(courseInfo.getCourseId(), promotion.getId());

                if (!isValid) {
                    log.warn("❌ Promotion code {} is invalid for course {}", promotionCode, courseInfo.getCourseName());
                    promotion = null;
                } else {
                    // Promotion hop le thi tinh so tien duoc giam va tang usage count
                    log.info("✅ Promotion code {} is valid for course {}", promotionCode, courseInfo.getCourseName());
                    discountAmount = promotionService.calculateDiscountAmount(coursePrice, promotion);
                    // Chan truong hop tien giam nhieu hon tien goc:
                    if (discountAmount.compareTo(coursePrice) > 0) {
                        discountAmount = coursePrice;
                    }
                }
            }

            log.info("✂️ Discount amount {} for course price {}", discountAmount, coursePrice);
            // Tinh gia tien cuoi cung cua khoa hoc
            BigDecimal finalPrice = coursePrice.subtract(discountAmount);
            BigDecimal commissionRate = courseInfo.getCommissionRate();
            totalPrice = totalPrice.add(finalPrice);

            // Tao va gan OrderDetail moi vao Order
            order.addOrderDetail(OrderDetail.builder()
                            .courseId(courseInfo.getCourseId())
                            .courseName(courseInfo.getCourseName())
                            .instructorId(courseInfo.getInstructorId())
                            .promotionId(promotion == null ? null : promotion.getId())
                            .originalPrice(coursePrice)
                            .discountAmount(discountAmount)
                            .finalPrice(finalPrice)
                            .commissionRate(commissionRate)
                            .build());
            log.info("👇 Final price is {}, total price is {} and commmissionRate is {}", finalPrice, totalPrice, commissionRate);
        }
        order.setTotalPrice(totalPrice);

        orderRepository.save(order);
        return new PendingOrderResponse(order.getId(), order.getTotalPrice());
    }

    @Override
    public List<CourseInternalRequest> getCoursesByOrderId(String orderId) {
        return List.of();
    }

    @Override
    @Transactional
    public void markAsPaid(String orderId) {
        // Cap nhat order status
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND,
                        "Order not found for id: " + orderId));
        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);

        // Lay danh sach promotion
        List<String> promotionIdList = order.getOrderDetails().stream().map(OrderDetail::getPromotionId)
                .filter(Objects::nonNull)
                .peek(id -> log.info("🆔 Promotion id {}", id))
                .toList();
        // Tang so luot dung cho tung promotion
        promotionService.increaseUsageCountBatch(promotionIdList);

        // Lay learnerId:
        String learnerId = order.getLearnerId();
        // Ban tin len order.exchange -> order.completed
        List<OrderCompletedMessage.OrderItemDto> items = order.getOrderDetails().stream().map(item -> OrderCompletedMessage.OrderItemDto.builder()
                .courseId(item.getCourseId())
                .instructorId(item.getInstructorId())
                .finalPrice(item.getFinalPrice())
                .paymentCurrency(DEFAULT_CURRENCY)
                .commissionRate(item.getCommissionRate() != null ? item.getCommissionRate() : BigDecimal.ZERO)
                .build()
        ).toList();

        OrderCompletedMessage message = new OrderCompletedMessage(orderId, learnerId, items);
        rabbitTemplate.convertAndSend(ORDER_EXCHANGE, ORDER_COMPLETED_ROUTING_KEY, message);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getMyOrders(String learnerId, Pageable pageable) {
        Page<Order> orderPage = orderRepository.findByLearnerIdOrderByCreatedAtDesc(learnerId, pageable);
        return orderPage.map(orderMapper::toOrderResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderDetailByLearner(String orderId, String learnerId) {
        Order order = orderRepository.findByIdAndLearnerId(orderId, learnerId)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND, "Không tìm thấy đơn hàng: " + orderId));
        return orderMapper.toOrderResponse(order);
    }
}