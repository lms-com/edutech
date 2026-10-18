package com.lms.order.controller;

import com.lms.common.dto.response.ApiResponse;
import com.lms.common.swagger.annotation.RequireJwt;
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
@RequireJwt
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

    @Operation(summary = "Gửi yêu cầu khiếu nại hoàn tiền (Chính sách 3 ngày)")
    @PostMapping("/{orderId}/refund-request")
    public ApiResponse<com.lms.order.dto.response.RefundRequestResponse> createRefundRequest(
            @PathVariable("orderId") String orderId,
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody com.lms.order.dto.request.CreateRefundRequest request
    ) {
        return ApiResponse.success(orderService.createRefundRequest(orderId, userId, request));
    }

    @Operation(summary = "Xem trạng thái khiếu nại của đơn hàng")
    @GetMapping("/{orderId}/refund-request")
    public ApiResponse<com.lms.order.dto.response.RefundRequestResponse> getRefundRequest(
            @PathVariable("orderId") String orderId,
            @RequestHeader("X-User-Id") String userId
    ) {
        return ApiResponse.success(orderService.getRefundRequestByOrder(orderId, userId));
    }

    @Operation(summary = "Admin: Lấy danh sách yêu cầu khiếu nại hoàn tiền")
    @GetMapping("/admin/refund-requests")
    public ApiResponse<org.springframework.data.domain.Page<com.lms.order.dto.response.RefundRequestResponse>> getAdminRefundRequests(
            @RequestParam(required = false) com.lms.order.model.RefundStatus status,
            @org.springframework.data.web.PageableDefault(size = 10, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC)
            org.springframework.data.domain.Pageable pageable
    ) {
        return ApiResponse.success(orderService.getAdminRefundRequests(status, pageable));
    }

    @Operation(summary = "Admin: Duyệt hoàn tiền đơn hàng")
    @PutMapping("/admin/refund-requests/{requestId}/approve")
    public ApiResponse<com.lms.order.dto.response.RefundRequestResponse> approveRefund(
            @PathVariable("requestId") String requestId,
            @RequestHeader(value = "X-User-Id", required = false) String adminId,
            @RequestBody(required = false) com.lms.order.dto.request.ReviewRefundRequest request
    ) {
        return ApiResponse.success(orderService.approveRefund(requestId, adminId != null ? adminId : "ADMIN", request));
    }

    @Operation(summary = "Admin: Từ chối hoàn tiền đơn hàng")
    @PutMapping("/admin/refund-requests/{requestId}/reject")
    public ApiResponse<com.lms.order.dto.response.RefundRequestResponse> rejectRefund(
            @PathVariable("requestId") String requestId,
            @RequestHeader(value = "X-User-Id", required = false) String adminId,
            @RequestBody(required = false) com.lms.order.dto.request.ReviewRefundRequest request
    ) {
        return ApiResponse.success(orderService.rejectRefund(requestId, adminId != null ? adminId : "ADMIN", request));
    }
}
