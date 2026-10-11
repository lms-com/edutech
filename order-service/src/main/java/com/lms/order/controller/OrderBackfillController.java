package com.lms.order.controller;

import com.lms.order.dto.message.OrderCompletedMessage;
import com.lms.order.model.Order;
import com.lms.order.model.OrderStatus;
import com.lms.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/internal/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderBackfillController {

    private final OrderRepository orderRepository;
    private final RabbitTemplate rabbitTemplate;

    // Khai báo lại tên Exchange và Routing Key cho chắc chắn
    private static final String ORDER_EXCHANGE = "order.exchange";
    private static final String ORDER_COMPLETED_ROUTING_KEY = "order.completed";

    @PostMapping("/sync-analytics")
    public String syncOldOrdersToAnalytics() {
        log.info("🚀 Bắt đầu tiến trình Backfill dữ liệu lịch sử sang Analytics...");

        // 1. Lấy TẤT CẢ đơn hàng đã thanh toán thành công trong quá khứ
        // Bạn nhớ viết hàm findByStatus trong OrderRepository nhé
        List<Order> completedOrders = orderRepository.findAllByStatus(OrderStatus.PAID);

        int count = 0;
        for (Order order : completedOrders) {
            try {
                // 2. Chuyển đổi item
                List<OrderCompletedMessage.OrderItemDto> items = order.getOrderDetails().stream()
                        .map(item -> OrderCompletedMessage.OrderItemDto.builder()
                                .courseId(item.getCourseId())
                                .instructorId(item.getInstructorId())
                                .finalPrice(item.getFinalPrice())
                                .paymentCurrency("VND")
                                .commissionRate(item.getCommissionRate() != null ? item.getCommissionRate() : BigDecimal.ZERO)
                                .build()
                        ).toList();

                // 3. Đóng gói message (Nhớ truyền đúng createdAt/paidAt của đơn hàng gốc)
                OrderCompletedMessage message = new OrderCompletedMessage(
                        order.getId(),
                        order.getLearnerId(),
                        order.getCreatedAt(), // THỜI GIAN GỐC CỦA ĐƠN HÀNG
                        items
                );

                // 4. Bắn lên RabbitMQ
                rabbitTemplate.convertAndSend(ORDER_EXCHANGE, ORDER_COMPLETED_ROUTING_KEY, message);
                count++;

            } catch (Exception e) {
                log.error("❌ Lỗi khi đồng bộ đơn hàng {}", order.getId(), e);
            }
        }

        log.info("✅ Đã đẩy thành công {} đơn hàng cũ lên RabbitMQ.", count);
        return "Đã đồng bộ " + count + " đơn hàng. Hãy kiểm tra log của analytics-service.";
    }
}
