package com.lms.order.service;

import com.lms.order.client.feign.course.dto.CourseInternalRequest;
import com.lms.order.dto.request.CreateOrderRequest;
import com.lms.order.dto.response.OrderResponse;
import com.lms.order.dto.response.PendingOrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OrderService {

    String createOrderAndGetPaymentUrl(CreateOrderRequest request, String userId);
    PendingOrderResponse createOrder (CreateOrderRequest request, String learnerId);
    List<CourseInternalRequest> getCoursesByOrderId (String orderId);
    void markAsPaid (String orderId);
    Page<OrderResponse> getMyOrders(String learnerId, Pageable pageable);
    OrderResponse getOrderDetailByLearner(String orderId, String learnerId);

    // Quản lý khiếu nại & hoàn tiền trong 3 ngày
    com.lms.order.dto.response.RefundRequestResponse createRefundRequest(String orderId, String learnerId, com.lms.order.dto.request.CreateRefundRequest request);
    com.lms.order.dto.response.RefundRequestResponse getRefundRequestByOrder(String orderId, String learnerId);
    Page<com.lms.order.dto.response.RefundRequestResponse> getAdminRefundRequests(com.lms.order.model.RefundStatus status, Pageable pageable);
    com.lms.order.dto.response.RefundRequestResponse approveRefund(String requestId, String adminId, com.lms.order.dto.request.ReviewRefundRequest request);
    com.lms.order.dto.response.RefundRequestResponse rejectRefund(String requestId, String adminId, com.lms.order.dto.request.ReviewRefundRequest request);
}
