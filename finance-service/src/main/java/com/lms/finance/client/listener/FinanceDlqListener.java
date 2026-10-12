package com.lms.finance.client.listener;

import com.lms.finance.entity.FailedSyncEvent;
import com.lms.finance.repository.FailedSyncEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class FinanceDlqListener {

    private final FailedSyncEventRepository failedSyncEventRepository;

    @RabbitListener(queues = "finance.dlq")
    public void processFailedMessages(Message failedMessage) {
        try {
            String payload = new String(failedMessage.getBody());
            String exchangeName = failedMessage.getMessageProperties().getReceivedExchange();
            String routingKey = failedMessage.getMessageProperties().getReceivedRoutingKey();

            String errorReason = "Unknown Error";
            Map<String, Object> headers = failedMessage.getMessageProperties().getHeaders();
            if (headers.containsKey("x-death")) {
                errorReason = headers.get("x-death").toString();
            }

            log.error("🚨 [FINANCE DLQ ALERT] Bắt được message lỗi! Lưu vào DB...");

            FailedSyncEvent event = FailedSyncEvent.builder()
                    .exchangeName(exchangeName)
                    .routingKey(routingKey)
                    .payload(payload)
                    .errorReason(errorReason)
                    .status("FAILED")
                    .build();

            failedSyncEventRepository.save(event);
            log.info("✅ Đã cất thông tin lỗi chia tiền vào bảng failed_sync_events.");

        } catch (Exception e) {
            log.error("❌ Lỗi khi cố gắng lưu DLQ Finance vào Database", e);
        }
    }
}