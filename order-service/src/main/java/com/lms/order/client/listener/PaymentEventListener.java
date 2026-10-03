package com.lms.order.client.listener;

import com.lms.order.dto.message.PaymentProcessMessage;
import com.lms.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static com.lms.order.config.RabbitMQConfig.PAYMENT_PROCESSED_QUEUE;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentEventListener {
    private final OrderService orderService;

    @RabbitListener(queues = PAYMENT_PROCESSED_QUEUE)
    public void onPaymentSuccess (PaymentProcessMessage message) {
        log.info("Received PaymentProcessMessage {}", message);
        try {
            // goi orderService cap nhat status thanh cong
            orderService.markAsPaid(message.getOrderId());
            log.info("✅ Payment successful for order {}",  message.getOrderId());
        } catch (Exception e) {
            log.error("❌ Payment failed for order {}",  message.getOrderId(), e);
            throw e;
        }
    }
}
