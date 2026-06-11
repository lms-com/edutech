package com.lms.notification.service;

public interface EmailService {
    // Hàm gửi email OTP bất đồng bộ, sử dụng @Async để không làm nghẽn Worker
    void sendOtpEmail(String referenceId, String recipientEmail, String otpCode);
}