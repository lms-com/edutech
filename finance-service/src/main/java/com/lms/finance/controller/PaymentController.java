package com.lms.finance.controller;

import com.lms.finance.config.VnPayConfig;
import com.lms.finance.dto.request.CreatePaymentRequest;
import com.lms.finance.service.PaymentService;
import com.lms.finance.service.impl.PaymentServiceImpl;
import com.lms.finance.util.VnPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final VnPayConfig vnPayConfig;
    private final PaymentService paymentService;

    /**
     * BƯỚC 1: API tạo link thanh toán (Logic ở PaymentService)
     * BƯỚC 2: API nhận phản hồi từ trình duyệt (Return URL)
     * Nhiệm vụ: Xử lý giao dịch, bắn RabbitMQ event kích hoạt khóa học và chuyển hướng (302) về Frontend
     */
    @GetMapping("/vnpay-callback")
    public ResponseEntity<Void> paymentCallback(@RequestParam Map<String, String> queryParams) {
        log.info("Nhận callback từ VNPAY Return URL: {}", queryParams);
        try {
            paymentService.handlePaymentCallback(queryParams);
        } catch (Exception e) {
            log.error("Lỗi khi xử lý thanh toán từ VNPay callback: ", e);
        }

        String targetFrontend = vnPayConfig.getFrontendReturnUrl();
        if (targetFrontend == null || targetFrontend.isBlank()) {
            targetFrontend = "http://localhost:5173";
        }

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(targetFrontend);
        queryParams.forEach(builder::queryParam);
        URI redirectUri = builder.build().toUri();

        log.info("Chuyển hướng trình duyệt về Frontend: {}", redirectUri);
        return ResponseEntity.status(HttpStatus.FOUND).location(redirectUri).build();
    }

    /**
     * BƯỚC 3: API nhận thông báo ngầm từ hệ thống VNPAY (IPN URL)
     * Nhiệm vụ: Kiểm tra bảo mật 4 bước nghiêm ngặt trước khi cập nhật Database
     */
    @GetMapping("/vnpay-ipn")
    public ResponseEntity<Map<String, String>> paymentIpn(@RequestParam Map<String, String> queryParams) {
        return ResponseEntity.ok(paymentService.handlePaymentCallback(queryParams));
    }
}
