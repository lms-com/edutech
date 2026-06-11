package com.lms.notification.event.consumer;

import com.lms.notification.config.RabbitMQConfig;
import com.lms.notification.enums.NotificationType;
import com.lms.notification.event.payload.SendOrderCompletedEvent;
import com.lms.notification.event.payload.SendOtpEvent;
import com.lms.notification.service.EmailService;
import com.lms.notification.service.NotificationService;

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
    NotificationService notificationService;

    // @RabbitListener nói với Spring Boot: "Hãy liên tục túc trực lắng nghe tại
    // Queue OTP này"
    // Cấu hình containerFactory (nếu có) hoặc mặc định sẽ tự bóc JSON nhờ
    // MessageConverter ở file Config
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

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NOTIFICATION_ORDER)
    public void listenOrderEvent(SendOrderCompletedEvent event) {
        log.info("➔ [RabbitMQ] Nhận được Event Order Completed. Đơn hàng: [{}], Học viên: [{}]",
                event.getOrderId(), event.getLearnerId());

        try {
            // Tác vụ 1: Gửi Email biên lai hóa đơn ngầm
            emailService.sendOrderSuccessEmail(
                    event.getOrderId(),
                    event.getLearnerEmail(),
                    event.getTotalAmount(),
                    event.getCourseIds());

            String title = "Thanh toán thành công !";
            String content = "Cảm ơn bạn đã đăng ký học ! Đơn hàng #" + event.getOrderId() + " đã được xử lý hoàn tất";

            // Tác vụ 2: Tạo thông báo lưu DB và kích hoạt đẩy SSE lên Quả Chuông Real-time
            notificationService.createAndSendNotification(
                    event.getLearnerId(),
                    title,
                    content,
                    NotificationType.ORDER_COMPLETED,
                    event.getOrderId(),
                    "Order");

            log.info("✔ Xử lý chuỗi sự kiện Đơn hàng [# {}] thành công trọn vẹn.", event.getOrderId());

        } catch (Exception e) {
            log.error("❌ Thất bại khi xử lý chuỗi sự kiện Đơn hàng [{}]. Chi tiết: {}", event.getOrderId(),
                    e.getMessage());
        }
    }
}