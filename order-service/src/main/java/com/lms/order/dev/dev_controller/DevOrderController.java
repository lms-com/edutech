package com.lms.order.dev.dev_controller;

import com.lms.common.dto.response.ApiResponse;
import com.lms.order.dev.dev_dto.DevOrderResponse;
import com.lms.order.dev.dev_service.DevOrderService;
import com.lms.order.model.Order;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "</> Dev Order APIs")
@RestController
@RequestMapping("/dev/orders")
@RequiredArgsConstructor
public class DevOrderController {
    private final DevOrderService devOrderService;

    @Operation(summary = "Get all Orders")
    @GetMapping
    public ApiResponse<List<Order>> getAllOrders(){
        return ApiResponse.success(devOrderService.getAllOrders());
    }

    @Operation(summary = "Get Order and items details")
    @GetMapping("/{id}")
    public ApiResponse<DevOrderResponse> getOrderById(@PathVariable String id){
        return ApiResponse.success(devOrderService.getOrder(id));
    }
}
