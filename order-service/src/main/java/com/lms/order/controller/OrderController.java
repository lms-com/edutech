package com.lms.order.controller;

import com.lms.common.dto.response.ApiResponse;
import com.lms.order.dto.request.CreateOrderRequest;
import com.lms.order.dto.response.OrderResponse;
import com.lms.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
@Tag(name = "Order Controller", description = "Apis serving ordering")
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "Create an Order", description = "Posting order request to create an order")
    @PostMapping
    public ApiResponse<Map<String, String>> createOrder (
            @Valid @RequestBody CreateOrderRequest request,
            @RequestHeader("X-User-Id") String userId
    ) {
        String vnPayUrl = orderService.createOrderAndGetPaymentUrl(request, userId);
        return ApiResponse.success(Map.of("paymentUrl", vnPayUrl));
    }

    @Operation(summary = "Lấy danh sách đơn hàng của tôi", description = "Trả về lịch sử đơn hàng của học viên có phân trang")
    @GetMapping("/me")
    public ApiResponse<org.springframework.data.domain.Page<OrderResponse>> getMyOrders(
            @RequestHeader("X-User-Id") String userId,
            @org.springframework.data.web.PageableDefault(size = 10, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC)
            org.springframework.data.domain.Pageable pageable
    ) {
        org.springframework.data.domain.Page<OrderResponse> orders = orderService.getMyOrders(userId, pageable);
        return ApiResponse.success(orders);
    }

    @Operation(summary = "Lấy chi tiết đơn hàng", description = "Trả về chi tiết đơn hàng theo orderId của học viên")
    @GetMapping("/{orderId}")
    public ApiResponse<OrderResponse> getOrderDetail(
            @PathVariable("orderId") String orderId,
            @RequestHeader("X-User-Id") String userId
    ) {
        OrderResponse order = orderService.getOrderDetailByLearner(orderId, userId);
        return ApiResponse.success(order);
    }
}
