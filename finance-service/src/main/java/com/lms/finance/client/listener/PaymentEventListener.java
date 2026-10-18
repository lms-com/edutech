package com.lms.finance.client.listener;

import com.lms.finance.dto.message.OrderCompletedMessage;
import com.lms.finance.service.RevenueShareService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import static com.lms.finance.config.RabbitMQConfig.ORDER_COMPLETED_QUEUE;


@Component
@Lazy(false)
@Slf4j
@RequiredArgsConstructor
public class PaymentEventListener {
    private final RevenueShareService revenueShareService;

    @RabbitListener(queues = ORDER_COMPLETED_QUEUE)
    public void onPaymentSuccess (OrderCompletedMessage message) {
        log.info("Received PaymentProcessMessage {}", message);
        if (message == null || message.getOrderId() == null || message.getItems() == null) {
            log.warn("⚠️ Bỏ qua OrderCompletedMessage không hợp lệ (null hoặc rỗng)");
            return;
        }
        try {
            revenueShareService.processRevenueDistribution(message);
            log.info("✅ Payment successful for order {}",  message.getOrderId());
        } catch (Exception e) {
            log.error("❌ Payment failed for order {}",  message.getOrderId(), e);
            throw e;
        }
    }
}
