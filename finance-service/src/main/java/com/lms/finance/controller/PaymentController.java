package com.lms.finance.controller;

import com.lms.finance.config.VnPayConfig;
import com.lms.finance.dto.request.CreatePaymentRequest;
import com.lms.finance.service.PaymentService;
import com.lms.finance.service.impl.PaymentServiceImpl;
import com.lms.finance.util.VnPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
     * BƯỚC 1: API tạo link thanh toán (Bạn đã hoàn thành logic ở Service trước đó)
     * BƯỚC 2: API nhận phản hồi từ trình duyệt (Return URL)
     * Nhiệm vụ: Đọc kết quả hiển thị giao diện thông báo cho người dùng
     */
    @GetMapping("/vnpay-callback")
    public ResponseEntity<Map<String, String>> paymentCallback(@RequestParam Map<String, String> queryParams) {
        String vnp_ResponseCode = queryParams.get("vnp_ResponseCode");
        Map<String, String> response = new HashMap<>();

        // Mã "00" nghĩa là người dùng đã bấm OTP thanh toán thành công tại giao diện VNPAY
        if ("00".equals(vnp_ResponseCode)) {
            response.put("status", "SUCCESS");
            response.put("message", "Thanh toan thành công! Trình duyệt sẽ chuyển bạn về trang chủ.");
        } else if ("10".equals(vnp_ResponseCode)) {
            response.put("status", "FAILED");
            response.put("message", "Bạn đã nhập sai thông tin quá 3 lần. Vui lòng quay lại màn hình đơn hàng để thực hiện lại giao dịch khác.");
        } else {
            response.put("status", "FAILED");
            response.put("message", "Giao dịch thất bại hoặc đã bị hủy. Mã lỗi: " + vnp_ResponseCode);
        }
        System.out.println("\033[43;32m" + response.toString() + "\n\033[0m");
        return ResponseEntity.ok(response);
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
