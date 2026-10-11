package com.lms.analytics.client.listener;

import com.lms.analytics.model.DailyCourseStatistic;
import com.lms.analytics.repository.DailyCourseStatisticRepository;
import com.lms.analytics.dto.message.OrderCompletedMessage; // Đảm bảo import đúng đường dẫn
import com.lms.common.util.TimeConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static com.lms.analytics.config.RabbitMQConfig.ANALYTICS_ORDER_COMPLETED_QUEUE;

@Component
@Lazy(value = false)
@Slf4j
@RequiredArgsConstructor
public class AnalyticsEventListener {

    private final DailyCourseStatisticRepository dailyStatisticRepo;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "analytics.order.completed.queue", durable = "true"),
            exchange = @Exchange(value = "order.exchange", type = "direct"),
            key = "order.completed"
    ))
    @Transactional
    public void onOrderCompleted(OrderCompletedMessage message) {
        log.info("📊 [Analytics] Đã nhận đơn hàng thành công, Order ID: {}", message.getOrderId());
        LocalDate recordDate = TimeConverter.toLocalDate(message.getCompletedAt(),ZoneId.systemDefault());

        try {
            // Đơn hàng có thể có nhiều item (mua nhiều khóa cùng lúc)
            for (OrderCompletedMessage.OrderItemDto item : message.getItems()) {
                String courseId = item.getCourseId();
                String instructorId = item.getInstructorId();
                BigDecimal finalPrice = item.getFinalPrice();
                BigDecimal commissionRate = item.getCommissionRate(); // Ví dụ: 0.2 (cho 20%)

                // Tính toán tiền
                BigDecimal platformFee = finalPrice.multiply(commissionRate);
                BigDecimal instructorRevenue = finalPrice.subtract(platformFee);

                // Tìm trong Database xem ngày hôm nay, khóa học này ĐÃ CÓ record thống kê chưa?
                Optional<DailyCourseStatistic> optStat = dailyStatisticRepo
                        .findByCourseIdAndRecordDate(courseId, recordDate);

                DailyCourseStatistic stat;
                if (optStat.isPresent()) {
                    // Đã có -> Cộng dồn
                    stat = optStat.get();
                    stat.setTotalOrderAmount(stat.getTotalOrderAmount().add(finalPrice));
                    stat.setInstructorRevenue(stat.getInstructorRevenue().add(instructorRevenue));
                    stat.setPlatformFee(stat.getPlatformFee().add(platformFee));
                    stat.setDailyEnrollments(stat.getDailyEnrollments() + 1);
                } else {
                    // Chưa có -> Tạo mới cho ngày hôm nay
                    stat = DailyCourseStatistic.builder()
                            .courseId(courseId)
                            .instructorId(instructorId)
                            .recordDate(recordDate)
                            .totalOrderAmount(finalPrice)
                            .instructorRevenue(instructorRevenue)
                            .platformFee(platformFee)
                            .dailyEnrollments(1)
                            // Các trường khác như refundedAmount đã set default ZERO ở Entity
                            .build();
                }

                // Lưu lại vào MySQL
                dailyStatisticRepo.save(stat);
            }
            log.info("✅ [Analytics] Cập nhật số liệu thành công cho Order ID: {}", message.getOrderId());

        } catch (Exception e) {
            log.error("❌ [Analytics] Lỗi khi cập nhật thống kê cho Order ID: {}", message.getOrderId(), e);
            // Có thể ném ra Exception để RabbitMQ retry nếu cần
            throw e;
        }
    }
}