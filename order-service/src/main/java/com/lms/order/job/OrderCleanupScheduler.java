package com.lms.order.job;

import com.lms.order.model.Order;
import com.lms.order.model.OrderStatus;
import com.lms.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@Lazy(false)
@RequiredArgsConstructor
@Slf4j
public class OrderCleanupScheduler {

    private final OrderRepository orderRepository;

    /**
     * Tự động quét và hủy các đơn hàng ở trạng thái PENDING quá 15 phút.
     * Chạy định kỳ mỗi 60 giây.
     */
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void cancelExpiredPendingOrders() {
        Instant fifteenMinutesAgo = Instant.now().minus(15, ChronoUnit.MINUTES);
        List<Order> expiredOrders = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PENDING, fifteenMinutesAgo);
        if (!expiredOrders.isEmpty()) {
            log.info("⏰ [OrderCleanupScheduler] Tự động hủy {} đơn hàng PENDING đã quá 15 phút", expiredOrders.size());
            for (Order order : expiredOrders) {
                order.setStatus(OrderStatus.CANCELLED);
            }
            orderRepository.saveAll(expiredOrders);
        }
    }
}
