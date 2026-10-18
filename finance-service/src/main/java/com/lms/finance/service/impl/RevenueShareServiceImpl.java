package com.lms.finance.service.impl;

import com.lms.finance.client.feign.order.OrderServiceFeignClient;
import com.lms.finance.client.feign.order.dto.CourseInternalRequest;
import com.lms.finance.dto.message.OrderCompletedMessage;
import com.lms.finance.dto.message.PaymentProcessMessage;
import com.lms.finance.entity.RevenueShare;
import com.lms.finance.enums.ReferenceType;
import com.lms.finance.enums.RevenueShareStatus;
import com.lms.finance.repository.RevenueShareRepository;
import com.lms.finance.service.InstructorBalanceService;
import com.lms.finance.service.RevenueShareService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;


import com.lms.finance.entity.BalanceHistory;
import com.lms.finance.entity.InstructorBalance;
import com.lms.finance.enums.EntryType;
import com.lms.finance.enums.PaymentStatus;
import com.lms.finance.enums.TransactionType;
import com.lms.finance.repository.BalanceHistoryRepository;
import com.lms.finance.repository.InstructorBalanceRepository;
import com.lms.finance.repository.PaymentRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class RevenueShareServiceImpl implements RevenueShareService {
    private final RabbitTemplate rabbitTemplate;
    private final OrderServiceFeignClient orderService;
    private final RevenueShareRepository revenueShareRepository;
    private final InstructorBalanceService balanceService;
    private final InstructorBalanceRepository balanceRepository;
    private final BalanceHistoryRepository balanceHistoryRepository;
    private final PaymentRepository paymentRepository;

    @Value("${application.finance.refund-deadline:3}")
    private String refundDeadline;

    @Override
    @Transactional
    public void processRevenueDistribution (OrderCompletedMessage message) {
        String orderId = message.getOrderId();
        // Tao RevenueShare cho thanh toan/chia tien:
        // Get List of courses from orderId
        List<OrderCompletedMessage.OrderItemDto> courses = message.getItems();  // thay bang feign client goi sang order-service
        Instant now = Instant.now();
        Instant releaseAt = now.plus(Integer.parseInt(refundDeadline), ChronoUnit.DAYS);
        for (OrderCompletedMessage.OrderItemDto course : courses) {
            if (revenueShareRepository.existsByOrderIdAndCourseId(orderId, course.getCourseId())) {
                log.info("⏩ RevenueShare đã tồn tại cho order {} và course {}. Bỏ qua để tránh xử lý trùng.",
                        orderId, course.getCourseId());
                continue;
            }

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
                        ReferenceType.REVENUE_SHARE.name(),
                        note
                );
        }
    }

    @Override
    @Transactional
    public void processRefund(String orderId) {
        log.info("🔄 Bắt đầu xử lý khấu trừ tài chính hoàn tiền cho đơn hàng [{}]", orderId);
        List<RevenueShare> shares = revenueShareRepository.findByOrderId(orderId);
        if (shares.isEmpty()) {
            log.warn("⚠️ Không tìm thấy bản ghi RevenueShare nào cho đơn hàng [{}]", orderId);
            return;
        }

        for (RevenueShare share : shares) {
            if (share.getStatus() == RevenueShareStatus.REFUNDED) {
                log.info("RevenueShare ID [{}] đã hoàn tiền trước đó, bỏ qua.", share.getId());
                continue;
            }

            InstructorBalance balance = balanceRepository.findByInstructorId(share.getInstructorId())
                    .orElse(null);

            if (balance != null) {
                BigDecimal pendingBefore = balance.getPendingBalance();
                BigDecimal availableBefore = balance.getAvailableBalance();
                BigDecimal blockedBefore = balance.getBlockedBalance();

                if (share.getStatus() == RevenueShareStatus.HOLDING) {
                    balance.deductRefundFromPending(share.getInstructorAmount());
                } else if (share.getStatus() == RevenueShareStatus.RELEASED) {
                    balance.setAvailableBalance(balance.getAvailableBalance().subtract(share.getInstructorAmount()));
                }

                String note = String.format("Khấu trừ hoàn tiền đơn hàng #%s - Khóa học %s (Chính sách 3 ngày)",
                        orderId.length() >= 8 ? orderId.substring(0, 8) : orderId, share.getCourseId());

                balanceHistoryRepository.save(BalanceHistory.createLog(
                        share.getInstructorId(),
                        balance,
                        EntryType.DEBIT,
                        TransactionType.REFUND_DEDUCTION,
                        share.getInstructorAmount().negate(),
                        pendingBefore,
                        availableBefore,
                        blockedBefore,
                        share.getId(),
                        ReferenceType.REVENUE_SHARE.name(),
                        note
                ));
                balanceRepository.save(balance);
                log.info("✔ Đã khấu trừ {} VND từ ví của giảng viên [{}]", share.getInstructorAmount(), share.getInstructorId());
            }

            share.setStatus(RevenueShareStatus.REFUNDED);
            revenueShareRepository.save(share);
        }

        // Cập nhật payment nếu có
        paymentRepository.findByOrderId(orderId).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(payment);
            log.info("✔ Đã cập nhật trạng thái Payment sang REFUNDED cho order [{}]", orderId);
        });

        log.info("✅ Hoàn tất xử lý hoàn tiền tài chính cho đơn hàng [{}]", orderId);
    }
}
