package com.lms.order.service;

import com.lms.order.client.feign.course.dto.CourseInternalRequest;
import com.lms.order.dto.request.CreateOrderRequest;
import com.lms.order.dto.response.PendingOrderResponse;

import java.util.List;

public interface OrderService {

    String createOrderAndGetPaymentUrl(CreateOrderRequest request, String userId);
    PendingOrderResponse createOrder (CreateOrderRequest request, String learnerId);
    List<CourseInternalRequest> getCoursesByOrderId (String orderId);
    void markAsPaid (String orderId);
}
