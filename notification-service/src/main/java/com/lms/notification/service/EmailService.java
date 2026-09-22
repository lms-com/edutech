package com.lms.notification.service;

import java.util.List;

public interface EmailService {
    // Hàm gửi email OTP bất đồng bộ, sử dụng @Async để không làm nghẽn Worker
    void sendOtpEmail(String referenceId, String recipientEmail, String otpCode);

    void sendOrderSuccessEmail(String orderId, String recipientEmail, Long totalAmount, List<String> courseIds);
}