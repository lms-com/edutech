package com.lms.order.service.impl;

import com.lms.order.client.feign.notification.NotificationFeignClient;
import com.lms.order.dto.request.CreateRefundRequest;
import com.lms.order.dto.request.ReviewRefundRequest;
import com.lms.order.dto.response.RefundRequestResponse;
import com.lms.order.model.RefundRequest;
import com.lms.order.model.RefundStatus;
import com.lms.order.repository.RefundRequestRepository;

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
    private final RefundRequestRepository refundRequestRepository;
    private final NotificationFeignClient notificationClient;
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
        if (order.getStatus() == OrderStatus.PAID) {
            log.info("Order {} is already PAID. Skipping duplicate processing.", orderId);
            return;
        }
        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);

        // Lay danh sach promotion
        List<String> promotionIdList = order.getOrderDetails().stream().map(OrderDetail::getPromotionId)
                .filter(Objects::nonNull)
                .peek(id -> log.info("🆔 Promotion id {}", id))
                .toList();
        // Tang so luot dung cho tung promotion
        if (!promotionIdList.isEmpty()) {
            promotionService.increaseUsageCountBatch(promotionIdList);
        }

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

    @Override
    @Transactional
    public RefundRequestResponse createRefundRequest(String orderId, String learnerId, CreateRefundRequest request) {
        log.info("📌 Học viên {} gửi yêu cầu khiếu nại / hoàn tiền cho đơn hàng {}", learnerId, orderId);
        Order order = orderRepository.findByIdAndLearnerId(orderId, learnerId)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND, "Không tìm thấy đơn hàng: " + orderId));

        if (order.getStatus() != OrderStatus.PAID) {
            throw new AppException(OrderErrorCode.ORDER_NOT_PAID);
        }

        // Kiểm tra chính sách 3 ngày
        Instant orderTime = order.getCreatedAt();
        long days = ChronoUnit.DAYS.between(orderTime, Instant.now());
        if (days > 3) {
            log.warn("⚠️ Đơn hàng {} đã quá 3 ngày ({} ngày), không thể yêu cầu hoàn tiền", orderId, days);
            throw new AppException(OrderErrorCode.REFUND_EXPIRED);
        }

        // Kiểm tra xem đã có khiếu nại đang chờ hay chưa
        refundRequestRepository.findByOrderIdAndStatus(orderId, RefundStatus.PENDING)
                .ifPresent(r -> {
                    throw new AppException(OrderErrorCode.REFUND_ALREADY_EXISTS);
                });

        RefundRequest refundRequest = RefundRequest.builder()
                .orderId(orderId)
                .learnerId(learnerId)
                .amount(order.getTotalPrice())
                .currencyCode(order.getCurrencyCode())
                .reason(request.getReason())
                .detail(request.getDetail())
                .status(RefundStatus.PENDING)
                .build();
        refundRequestRepository.save(refundRequest);

        order.setStatus(OrderStatus.REFUND_REQUESTED);
        orderRepository.save(order);

        // Bắn thông báo xác nhận đã tiếp nhận khiếu nại cho học viên
        try {
            notificationClient.sendCustomNotification(
                    learnerId,
                    "Tiếp nhận yêu cầu khiếu nại hoàn tiền",
                    String.format("Yêu cầu hoàn tiền đơn hàng #%s (%s đ) đã được gửi đến Ban Quản Trị để thẩm định theo chính sách 3 ngày.",
                            orderId.length() >= 8 ? orderId.substring(0, 8) : orderId, order.getTotalPrice().toPlainString()),
                    "ORDER_COMPLETED",
                    orderId,
                    "Order"
            );
        } catch (Exception e) {
            log.error("Không thể gửi thông báo cho học viên: {}", e.getMessage());
        }

        log.info("✔ Tạo yêu cầu hoàn tiền thành công ID: {} cho đơn hàng {}", refundRequest.getId(), orderId);
        return toRefundResponse(refundRequest, order);
    }

    @Override
    @Transactional(readOnly = true)
    public RefundRequestResponse getRefundRequestByOrder(String orderId, String learnerId) {
        Order order = orderRepository.findByIdAndLearnerId(orderId, learnerId)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND, "Không tìm thấy đơn hàng: " + orderId));
        RefundRequest req = refundRequestRepository.findByOrderId(orderId)
                .orElseThrow(() -> new AppException(OrderErrorCode.REFUND_NOT_FOUND));
        return toRefundResponse(req, order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RefundRequestResponse> getAdminRefundRequests(RefundStatus status, Pageable pageable) {
        Page<RefundRequest> page = (status != null)
                ? refundRequestRepository.findByStatus(status, pageable)
                : refundRequestRepository.findAllByOrderByCreatedAtDesc(pageable);

        return page.map(req -> {
            Order order = orderRepository.findById(req.getOrderId()).orElse(null);
            return toRefundResponse(req, order);
        });
    }

    @Override
    @Transactional
    public RefundRequestResponse approveRefund(String requestId, String adminId, ReviewRefundRequest request) {
        log.info("⭐ Admin {} phê duyệt yêu cầu hoàn tiền {}", adminId, requestId);
        RefundRequest refundReq = refundRequestRepository.findById(requestId)
                .orElseThrow(() -> new AppException(OrderErrorCode.REFUND_NOT_FOUND));
        Order order = orderRepository.findById(refundReq.getOrderId())
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));

        refundReq.setStatus(RefundStatus.APPROVED);
        refundReq.setResolvedBy(adminId);
        refundReq.setResolvedAt(Instant.now());
        refundReq.setAdminNote(request != null && request.getNote() != null && !request.getNote().isBlank()
                ? request.getNote() : "Chấp thuận yêu cầu hoàn tiền theo chính sách 3 ngày");
        refundRequestRepository.save(refundReq);

        order.setStatus(OrderStatus.REFUNDED);
        orderRepository.save(order);

        // 1. Gọi finance-service khấu trừ ví Pending của giảng viên
        try {
            financeClient.refundOrder(order.getId());
            log.info("✔ Finance refund executed successfully for order: {}", order.getId());
        } catch (Exception e) {
            log.error("❌ Lỗi khi gọi financeClient.refundOrder: {}", e.getMessage());
        }

        // 2. Thu hồi quyền học trong enrollment-service cho tất cả khóa trong đơn
        if (order.getOrderDetails() != null) {
            for (OrderDetail item : order.getOrderDetails()) {
                try {
                    enrollmentClient.revokeAccess(order.getLearnerId(), item.getCourseId());
                    log.info("✔ Đã thu hồi quyền học của học viên {} đối với khóa học {}", order.getLearnerId(), item.getCourseId());
                } catch (Exception e) {
                    log.error("❌ Lỗi khi gọi enrollmentClient.revokeAccess: {}", e.getMessage());
                }
            }
        }

        // 3. Gửi thông báo real-time SSE cho Học viên và từng Giảng viên
        try {
            String shortOrderId = order.getId().length() >= 8 ? order.getId().substring(0, 8) : order.getId();
            notificationClient.sendCustomNotification(
                    order.getLearnerId(),
                    "Yêu cầu hoàn tiền đã được chấp thuận!",
                    String.format("Đơn hàng #%s (%s đ) đã được Admin duyệt hoàn tiền. Khóa học đã được thu hồi.",
                            shortOrderId, order.getTotalPrice().toPlainString()),
                    "REFUND_APPROVED",
                    order.getId(),
                    "Order"
            );

            // Bắn thông báo cho từng giảng viên
            if (order.getOrderDetails() != null) {
                for (OrderDetail item : order.getOrderDetails()) {
                    if (item.getInstructorId() != null && !item.getInstructorId().isBlank()) {
                        notificationClient.sendCustomNotification(
                                item.getInstructorId(),
                                "Đơn hàng đã hoàn tiền (Chính sách 3 ngày)",
                                String.format("Khóa học [%s] thuộc đơn hàng #%s đã được hoàn tiền cho học viên. Doanh thu tạm giữ tương ứng đã được khấu trừ.",
                                        item.getCourseName(), shortOrderId),
                                "REFUND_APPROVED",
                                order.getId(),
                                "Order"
                        );
                    }
                }
            }
        } catch (Exception e) {
            log.error("Lỗi khi bắn thông báo duyệt hoàn tiền: {}", e.getMessage());
        }

        return toRefundResponse(refundReq, order);
    }

    @Override
    @Transactional
    public RefundRequestResponse rejectRefund(String requestId, String adminId, ReviewRefundRequest request) {
        log.info("⛔ Admin {} từ chối yêu cầu hoàn tiền {}", adminId, requestId);
        RefundRequest refundReq = refundRequestRepository.findById(requestId)
                .orElseThrow(() -> new AppException(OrderErrorCode.REFUND_NOT_FOUND));
        Order order = orderRepository.findById(refundReq.getOrderId())
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));

        String rejectReason = request != null && request.getNote() != null && !request.getNote().isBlank()
                ? request.getNote() : "Không thỏa mãn điều kiện chính sách hoàn tiền 3 ngày";

        refundReq.setStatus(RefundStatus.REJECTED);
        refundReq.setResolvedBy(adminId);
        refundReq.setResolvedAt(Instant.now());
        refundReq.setAdminNote(rejectReason);
        refundRequestRepository.save(refundReq);

        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);

        // Bắn thông báo từ chối cho Học viên
        try {
            String shortOrderId = order.getId().length() >= 8 ? order.getId().substring(0, 8) : order.getId();
            notificationClient.sendCustomNotification(
                    order.getLearnerId(),
                    "Yêu cầu hoàn tiền bị từ chối",
                    String.format("Yêu cầu hoàn tiền đơn hàng #%s bị từ chối. Lý do: %s.",
                            shortOrderId, rejectReason),
                    "REFUND_REJECTED",
                    order.getId(),
                    "Order"
            );
        } catch (Exception e) {
            log.error("Lỗi khi bắn thông báo từ chối hoàn tiền: {}", e.getMessage());
        }

        return toRefundResponse(refundReq, order);
    }

    private RefundRequestResponse toRefundResponse(RefundRequest req, Order order) {
        List<RefundRequestResponse.RefundOrderItemDto> items = null;
        Instant orderCreatedAt = null;
        if (order != null) {
            orderCreatedAt = order.getCreatedAt();
            if (order.getOrderDetails() != null) {
                items = order.getOrderDetails().stream().map(d -> RefundRequestResponse.RefundOrderItemDto.builder()
                        .courseId(d.getCourseId())
                        .courseName(d.getCourseName())
                        .instructorId(d.getInstructorId())
                        .finalPrice(d.getFinalPrice())
                        .build()
                ).toList();
            }
        }
        return RefundRequestResponse.builder()
                .id(req.getId())
                .orderId(req.getOrderId())
                .learnerId(req.getLearnerId())
                .amount(req.getAmount())
                .currencyCode(req.getCurrencyCode())
                .reason(req.getReason())
                .detail(req.getDetail())
                .status(req.getStatus())
                .adminNote(req.getAdminNote())
                .resolvedBy(req.getResolvedBy())
                .resolvedAt(req.getResolvedAt())
                .createdAt(req.getCreatedAt())
                .orderCreatedAt(orderCreatedAt)
                .items(items)
                .build();
    }
}
