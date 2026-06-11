package com.lms.notification.service.impl;

import com.lms.notification.entity.EmailLog;
import com.lms.notification.enums.EmailStatus;
import com.lms.notification.repository.EmailLogRepository;
import com.lms.notification.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class EmailServiceImpl implements EmailService {

    JavaMailSender mailSender;
    TemplateEngine templateEngine; // Thư viện của Spring để đọc và chèn biến vào file HTML
    EmailLogRepository emailLogRepository;

    @Override
    @Async
    public void sendOtpEmail(String referenceId, String recipientEmail, String otpCode) {
        log.info("Bắt đầu tiến trình gửi Email OTP ngầm cho: [{}]", recipientEmail);

        // 1. Khởi tạo log email ở trạng thái chờ gửi
        EmailLog emailLog = EmailLog.builder()
                .recipientEmail(recipientEmail)
                .subject("Mã xác thực OTP - EDUTECH LMS")
                .templateName("otp.html")
                .referenceId(referenceId)
                .referenceType("AUTH_OTP")
                .build();

        try {
            // 2. Sử dụng Thymeleaf để đổ dữ liệu OTP vào file HTML
            Context context = new Context();
            context.setVariable("otpCode", otpCode);
            String htmlContent = templateEngine.process("email/otp", context);

            // 3. Cấu hình Email Mime (Hỗ trợ gửi định dạng HTML thay vì chữ thô)
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(recipientEmail);
            helper.setSubject(emailLog.getSubject());
            helper.setText(htmlContent, true); // true nghĩa là cho phép render HTML

            // 4. Thực tế bắn email đi qua SMTP Server
            mailSender.send(message);

            // 5. Cập nhật log gửi thành công
            emailLog.setStatus(EmailStatus.SENT);
            emailLog.setSentAt(LocalDateTime.now());
            log.info("Đã gửi Email OTP thành công tới: [{}]", recipientEmail);

        } catch (Exception e) {
            // Nếu lỗi (Sai pass SMTP, nghẽn mạng...), bắt ngoại lệ và lưu log FAILED để Admin vào kiểm tra
            emailLog.setStatus(EmailStatus.FAILED);
            emailLog.setErrorMessage(e.getMessage());
            log.error("Thất bại khi gửi Email OTP cho [{}]. Lý do: {}", recipientEmail, e.getMessage());
        } finally {
            emailLogRepository.save(emailLog);
        }
    }
}