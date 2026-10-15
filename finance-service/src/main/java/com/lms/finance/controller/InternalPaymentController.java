package com.lms.finance.controller;

import com.lms.common.swagger.annotation.RequireInternalKey;
import com.lms.finance.dto.request.CreatePaymentRequest;
import com.lms.finance.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lms.common.dto.response.ApiResponse;
import com.lms.finance.service.RevenueShareService;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/internal/v1/payments")
@RequireInternalKey
@RequiredArgsConstructor
public class InternalPaymentController {
    private final PaymentService paymentService;
    private final RevenueShareService revenueShareService;

    @PostMapping("/create")
    public ResponseEntity<String> createPayment(
            HttpServletRequest httpReq,
            @RequestBody CreatePaymentRequest createReq) {
        return ResponseEntity.ok(paymentService.createPayment(httpReq, createReq));
    }

    @PostMapping("/refund/{orderId}")
    public ResponseEntity<ApiResponse<Void>> refundOrder(@PathVariable String orderId) {
        revenueShareService.processRefund(orderId);
        return ResponseEntity.ok(ApiResponse.success(null, "Processed refund successfully"));
    }
}
