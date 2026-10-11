package com.lms.finance.publisher;

import com.lms.finance.dto.message.BalanceUpdatedEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class FinanceRabbitMQPublisher {

    private final RabbitTemplate rabbitTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBalanceUpdatedEvent(BalanceUpdatedEventMessage event) {
        try {
            Map<String, Object> message = Map.of(
                    "instructorId", event.getInstructorId(),
                    "availableBalance", event.getAvailableBalance()
            );

            rabbitTemplate.convertAndSend("finance.exchange", "finance.balance.updated", message);
            log.info("📤 Đã bắn Event đồng bộ số dư của Giảng viên {} sang Analytics.", event.getInstructorId());
        } catch (Exception e) {
            log.error("❌ Lỗi khi bắn Event đồng bộ số dư", e);
        }
    }
}
