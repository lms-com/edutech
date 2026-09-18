package com.lms.finance.controller;

import com.lms.finance.dto.request.CreatePaymentRequest;
import com.lms.finance.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/v1/payments")
@RequiredArgsConstructor
public class InternalPaymentController {
    private final PaymentService paymentService;

    @PostMapping("/create")
    public ResponseEntity<String> createPayment(
            HttpServletRequest httpReq,
            @RequestBody CreatePaymentRequest createReq) {
        return ResponseEntity.ok(paymentService.createPayment(httpReq, createReq));
    }
}
