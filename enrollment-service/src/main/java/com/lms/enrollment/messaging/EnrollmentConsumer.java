package com.lms.enrollment.messaging;

import com.lms.enrollment.dto.event.OrderCompletedEvent;
import com.lms.enrollment.service.EnrollmentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class EnrollmentConsumer {

    EnrollmentService enrollmentService;

    @RabbitListener(queues = "enrollment.order.completed.queue")
    public void handleOrderCompleted(OrderCompletedEvent event) {
        log.info("Nhận event order.completed — orderId={}, learnerId={}",
            event.getOrderId(), event.getLearnerId());
        try {
            enrollmentService.enrollFromOrder(event);
        } catch (DataIntegrityViolationException e) {
            // UNIQUE(learner_id, course_id) throw khi duplicate event
            // Bỏ qua — đây là idempotent consumer, không cần xử lý lại
            log.warn("Duplicate enrollment event bỏ qua — orderId={}", event.getOrderId());
        }
    }
}
