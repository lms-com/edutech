package com.lms.analytics.client.listener;

import com.lms.analytics.dto.message.OrderCompletedMessage;
import com.lms.analytics.service.AnalyticsOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderDataSyncListener {

    private final AnalyticsOrderService analyticsOrderService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(
                    value = "analytics.order.completed.queue",
                    durable = "true",
                    arguments = {
                            @Argument(name = "x-dead-letter-exchange", value = "analytics.dlx"),
                            @Argument(name = "x-dead-letter-routing-key", value = "analytics.dlq.routing-key")
                    }
            ),
            exchange = @Exchange(value = "order.exchange", type = "direct"),
            key = "order.completed"
    ))
    public void onOrderCompleted(OrderCompletedMessage message) {
        try {
            log.info("📥 [RabbitMQ] Nhận event order.completed từ đơn hàng: {}", message.getOrderId());
            analyticsOrderService.processOrderCompleted(message);
        } catch (Exception e) {
            log.error("❌ Lỗi khi xử lý event order.completed cho đơn hàng: {}", message.getOrderId(), e);
            throw e; // Bắt buộc ném exception ra ngoài để Spring Retry đếm số lần và đẩy sang DLQ
        }
    }
}
