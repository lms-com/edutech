package com.lms.notification.event.consumer;

import com.lms.common.dto.response.ApiResponse;
import com.lms.notification.client.IamServiceClient;
import com.lms.notification.config.RabbitMQConfig;
import com.lms.notification.dto.response.LearnerInfoResponse;
import com.lms.notification.enums.NotificationType;
import com.lms.notification.event.payload.CourseStatusChangedEvent;
import com.lms.notification.event.payload.SendOrderCompletedEvent;
import com.lms.notification.event.payload.SendOtpEvent;
import com.lms.notification.service.EmailService;
import com.lms.notification.service.NotificationService;
import com.lms.notification.event.payload.CourseCompletedEvent;
import com.lms.notification.service.CertificateService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class NotificationConsumer {

    EmailService emailService;
    NotificationService notificationService;
    CertificateService certificateService;
    IamServiceClient iamServiceClient;

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

        // Tác vụ 1: Tạo thông báo lưu DB và kích hoạt đẩy SSE lên Quả Chuông Real-time.
        // Tách riêng khỏi bước gửi mail để việc tra cứu email qua IAM có lỗi cũng
        // không làm mất thông báo trên chuông.
        try {
            notificationService.createAndSendNotification(
                    event.getLearnerId(),
                    "Thanh toán thành công !",
                    "Cảm ơn bạn đã đăng ký học ! Đơn hàng #" + event.getOrderId() + " đã được xử lý hoàn tất",
                    NotificationType.ORDER_COMPLETED,
                    event.getOrderId(),
                    "Order");
        } catch (Exception e) {
            log.error("❌ Thất bại khi tạo thông báo cho Đơn hàng [{}]. Chi tiết: {}", event.getOrderId(),
                    e.getMessage());
        }

        // Tác vụ 2: Gửi Email biên lai hóa đơn ngầm
        try {
            emailService.sendOrderSuccessEmail(
                    event.getOrderId(),
                    resolveLearnerEmail(event.getLearnerId()),
                    totalAmountOf(event),
                    courseIdsOf(event));

            log.info("✔ Xử lý chuỗi sự kiện Đơn hàng [# {}] thành công trọn vẹn.", event.getOrderId());

        } catch (Exception e) {
            log.error("❌ Thất bại khi gửi email biên lai Đơn hàng [{}]. Chi tiết: {}", event.getOrderId(),
                    e.getMessage());
        }
    }

    /** Tra email học viên qua iam-service vì bản tin order.completed không mang theo email. */
    private String resolveLearnerEmail(String learnerId) {
        ApiResponse<List<LearnerInfoResponse>> response = iamServiceClient.getUserByIds(List.of(learnerId));
        if (response == null || response.getData() == null || response.getData().isEmpty()) {
            throw new IllegalStateException("Không tìm thấy thông tin học viên từ iam-service: " + learnerId);
        }
        return response.getData().get(0).getEmail();
    }

    private List<String> courseIdsOf(SendOrderCompletedEvent event) {
        if (event.getItems() == null) {
            return List.of();
        }
        return event.getItems().stream()
                .map(SendOrderCompletedEvent.OrderItem::getCourseId)
                .filter(Objects::nonNull)
                .toList();
    }

    private Long totalAmountOf(SendOrderCompletedEvent event) {
        if (event.getItems() == null) {
            return 0L;
        }
        return event.getItems().stream()
                .map(SendOrderCompletedEvent.OrderItem::getFinalPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .longValue();
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NOTIFICATION_COURSE_STATUS)
    public void listenCourseStatusEvent(CourseStatusChangedEvent event) {
        log.info("➔ [RabbitMQ] Nhận được Event Course Status Changed. Khóa học: [{}], Trạng thái mới: [{}]",
                event.getCourseTitle(), event.getStatus());
        try {
            String title;
            String content;
            NotificationType type;
            if ("APPROVED".equalsIgnoreCase(event.getStatus())) {
                title = "Khóa học đã được duyệt !";
                content = "Chúc mừng! Khóa học \"" + event.getCourseTitle()
                        + "\" của bạn đã được Admin phê duyệt và xuất bản.";
                type = NotificationType.COURSE_APPROVED;
            } else {
                title = "Yêu cầu duyệt khóa học bị từ chối";
                content = "Khóa học \"" + event.getCourseTitle() + "\" của bạn không được phê duyệt. Lý do: "
                        + (event.getRejectionNote() != null ? event.getRejectionNote() : "Không có lý do cụ thể.");
                type = NotificationType.COURSE_REJECTED;
            }
            // Gửi thông báo in-app (lưu DB & đẩy SSE real-time lên quả chuông của
            // Instructor)
            notificationService.createAndSendNotification(
                    event.getInstructorId(),
                    title,
                    content,
                    type,
                    event.getCourseId(),
                    "Course");
        } catch (Exception e) {
            log.error("❌ Thất bại khi xử lý thông báo trạng thái khóa học [{}]. Chi tiết: {}", event.getCourseId(),
                    e.getMessage());
        }
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NOTIFICATION_COURSE_COMPLETED)
    public void listenCourseCompletedEvent(CourseCompletedEvent event) {
        log.info("➔ [RabbitMQ] Nhận được Event Course Completed. Học viên: [{}], Khóa học: [{}]",
                event.getLearnerId(), event.getCourseId());
        try {
            // Kích hoạt quy trình sinh chứng chỉ PDF, upload MinIO & lưu DB
            certificateService.generateCertificate(
                    event.getLearnerId(),
                    event.getCourseId(),
                    event.getEnrollmentId());
            log.info("✔ Cấp chứng chỉ thành công cho học viên [{}]", event.getLearnerId());
        } catch (Exception e) {
            log.error("❌ Thất bại khi xử lý sinh chứng chỉ cho học viên [{}]. Chi tiết: {}",
                    event.getLearnerId(), e.getMessage());
        }
    }

}