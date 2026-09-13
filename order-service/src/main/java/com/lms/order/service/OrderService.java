package com.lms.order.service;

import com.lms.order.dto.request.CreateOrderRequest;
import com.lms.order.dto.response.OrderResponse;
import com.lms.order.model.Order;

public interface OrderService {

    OrderResponse createOrder (CreateOrderRequest request, String learnerId);
}
