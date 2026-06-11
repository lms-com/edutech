package com.lms.notification.event.consumer;

import com.lms.notification.config.RabbitMQConfig;
import com.lms.notification.event.payload.SendOtpEvent;
import com.lms.notification.service.EmailService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class NotificationConsumer {

    EmailService emailService;

    // @RabbitListener nói với Spring Boot: "Hãy liên tục túc trực lắng nghe tại Queue OTP này"
    // Cấu hình containerFactory (nếu có) hoặc mặc định sẽ tự bóc JSON nhờ MessageConverter ở file Config
    @RabbitListener(queues = RabbitMQConfig.QUEUE_NOTIFICATION_OTP)
    public void listenOtpEvent(SendOtpEvent event) {
        log.info("➔ [RabbitMQ] Đã nhận được Event OTP từ IAM-SERVICE. Người nhận: [{}]", event.getEmail());

        try {
            // Tạo một ID ngẫu nhiên để làm ReferenceID lưu vết trong bảng email_logs
            String referenceId = UUID.randomUUID().toString();

            // Gọi sang tầng Service xử lý gửi mail ngầm
            emailService.sendOtpEmail(referenceId, event.getEmail(), event.getOtpCode());

        } catch (Exception e) {
            log.error("❌ Lỗi xảy ra khi Worker xử lý tin nhắn OTP: {}", e.getMessage());
        }
    }
}