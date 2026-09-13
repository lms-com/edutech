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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
@Tag(name = "Order Controller", description = "Apis serving ordering")
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "Create an Order", description = "Posting order request to create an order")
    @PostMapping
    public ApiResponse<OrderResponse> createOrder (
            @Valid @RequestBody CreateOrderRequest request,
            @RequestHeader("X-User-Id") String userId
    ) {
        return ApiResponse.success(orderService.createOrder(request,userId));
    }
}
