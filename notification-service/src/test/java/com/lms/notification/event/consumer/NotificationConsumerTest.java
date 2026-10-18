package com.lms.notification.event.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.common.dto.response.ApiResponse;
import com.lms.notification.client.IamServiceClient;
import com.lms.notification.dto.response.LearnerInfoResponse;
import com.lms.notification.enums.NotificationType;
import com.lms.notification.event.payload.SendOrderCompletedEvent;
import com.lms.notification.service.CertificateService;
import com.lms.notification.service.EmailService;
import com.lms.notification.service.NotificationService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Khoá contract bản tin order.exchange / order.completed giữa order-service và
 * notification-service. JSON dưới đây là đúng hình dạng OrderCompletedMessage
 * mà order-service phát ra; đổi field bên order mà không cập nhật bên này sẽ
 * làm test đỏ.
 */
class NotificationConsumerTest {

    static final String ORDER_COMPLETED_JSON = """
            {
              "orderId": "order-1",
              "learnerId": "learner-1",
              "items": [
                {"courseId": "course-1", "instructorId": "ins-1", "finalPrice": 150000,
                 "paymentCurrency": "VND", "commissionRate": 0.3},
                {"courseId": "course-2", "instructorId": "ins-2", "finalPrice": 250000,
                 "paymentCurrency": "VND", "commissionRate": 0.3}
              ]
            }
            """;

    final EmailService emailService = mock(EmailService.class);
    final NotificationService notificationService = mock(NotificationService.class);
    final CertificateService certificateService = mock(CertificateService.class);
    final IamServiceClient iamServiceClient = mock(IamServiceClient.class);

    final NotificationConsumer consumer = new NotificationConsumer(
            emailService, notificationService, certificateService, iamServiceClient);

    @Test
    void bocDungBanTinOrderVaGuiEmailBienLai() throws Exception {
        SendOrderCompletedEvent event = new ObjectMapper()
                .readValue(ORDER_COMPLETED_JSON, SendOrderCompletedEvent.class);

        LearnerInfoResponse learner = new LearnerInfoResponse();
        learner.setId("learner-1");
        learner.setEmail("hocvien@lms.com");
        when(iamServiceClient.getUserByIds(List.of("learner-1")))
                .thenReturn(ApiResponse.success(List.of(learner)));

        consumer.listenOrderEvent(event);

        // Thông báo in-app cho quả chuông
        verify(notificationService).createAndSendNotification(
                eq("learner-1"),
                anyString(),
                contains("order-1"),
                eq(NotificationType.ORDER_COMPLETED),
                eq("order-1"),
                eq("Order"));

        // Email biên lai: email tra qua IAM, tổng tiền và courseIds suy ra từ items
        verify(emailService).sendOrderSuccessEmail(
                "order-1",
                "hocvien@lms.com",
                400000L,
                List.of("course-1", "course-2"));
    }

    @Test
    void vanTaoThongBaoKhiTraEmailQuaIamThatBai() throws Exception {
        SendOrderCompletedEvent event = new ObjectMapper()
                .readValue(ORDER_COMPLETED_JSON, SendOrderCompletedEvent.class);

        when(iamServiceClient.getUserByIds(List.of("learner-1")))
                .thenThrow(new RuntimeException("iam-service không phản hồi"));

        // Lỗi tra email không được làm mất thông báo trên chuông, và không được ném ra ngoài
        assertThatCode(() -> consumer.listenOrderEvent(event)).doesNotThrowAnyException();

        verify(notificationService).createAndSendNotification(
                eq("learner-1"), anyString(), contains("order-1"),
                eq(NotificationType.ORDER_COMPLETED), eq("order-1"), eq("Order"));
    }
}
