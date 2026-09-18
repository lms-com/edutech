package com.lms.finance.service.impl;

import com.lms.finance.client.feign.order.OrderServiceFeignClient;
import com.lms.finance.client.feign.order.dto.CourseInternalRequest;
import com.lms.finance.dto.message.OrderCompletedMessage;
import com.lms.finance.dto.message.PaymentProcessMessage;
import com.lms.finance.entity.RevenueShare;
import com.lms.finance.enums.RevenueShareStatus;
import com.lms.finance.repository.RevenueShareRepository;
import com.lms.finance.service.InstructorBalanceService;
import com.lms.finance.service.RevenueShareService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class RevenueShareServiceImpl implements RevenueShareService {
    private final RabbitTemplate rabbitTemplate;
    private final OrderServiceFeignClient orderService;
    private final RevenueShareRepository revenueShareRepository;
    private final InstructorBalanceService balanceService;

    @Override
    @Transactional
    public void processRevenueDistribution (OrderCompletedMessage message) {
        String orderId = message.getOrderId();
        // Tao RevenueShare cho thanh toan/chia tien:
        // Get List of courses from orderId
        List<OrderCompletedMessage.OrderItemDto> courses = message.getItems();  // thay bang feign client goi sang order-service
        Instant now = Instant.now();
        Instant releaseAt = now.plus(7, ChronoUnit.DAYS);
        for (OrderCompletedMessage.OrderItemDto course : courses) {
            BigDecimal grossAmount = course.getFinalPrice();
            BigDecimal commissionRate = course.getCommissionRate();
            BigDecimal instructorAmount = grossAmount.multiply(commissionRate);
            BigDecimal platformFee = grossAmount.subtract(grossAmount.multiply(commissionRate));

            log.info("📉 Revenue share for course {}\n" +
                    "➡️ gross = {}\n" + "➡️ commissionRate = {}\n" + "➡️ instructorAmount = {}\n" +
                    "➡️ platformFee = {}\n",
                    course.getCourseId(), grossAmount.floatValue(), commissionRate.floatValue(),
                    instructorAmount.floatValue(), platformFee.floatValue());
            RevenueShare revenueShare = revenueShareRepository.saveAndFlush(
                    RevenueShare.builder()
                        .orderId(orderId)
                        .courseId(course.getCourseId())
                        .instructorId(course.getInstructorId())
                        .grossAmount(grossAmount)
                        .currencyCode(course.getPaymentCurrency())
                        .commissionRate(commissionRate)
                        .instructorAmount(instructorAmount)
                        .platformFee(platformFee)
                        .status(RevenueShareStatus.HOLDING)
                        .idempotencyKey(String.format("%s:%s:%s", orderId, course.getCourseId(), RevenueShareStatus.HOLDING.name()))  // orderId:courseId:status
                        .releaseAt(releaseAt)
                        .build()
            );

                // Bắn sang service chuyên trách quản lý Ví để cộng tiền pending và ghi log lịch sử
                String note = String.format("Doanh thu tạm giữ khóa học %s - Đơn hàng #%s",
                        course.getCourseId(), message.getOrderId().substring(0, 8));

                log.info("✂️ Call function to deposit revenueShare for instructor {} amount {}",
                        course.getInstructorId(),
                        revenueShare.getInstructorAmount()
                );
                // Cap nhat instructor balance:
                log.info("➡️ Instructor amount from RevenueShare {}  and from course {}", revenueShare.getInstructorAmount(), instructorAmount);
                balanceService.depositToPendingBalance(
                        instructorAmount,
                        revenueShare.getCurrencyCode(),
                        revenueShare.getInstructorId(),
                        revenueShare.getId(),
                        "REVENUE_SHARE",
                        note
                );
        }
    }
}
