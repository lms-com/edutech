package com.lms.analytics.service.impl;

import com.lms.analytics.service.AnalyticsOrderService;
import com.lms.analytics.dto.message.OrderCompletedMessage;
import com.lms.analytics.model.DailyCourseStatistic;
import com.lms.analytics.repository.DailyCourseStatisticRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsOrderServiceImpl implements AnalyticsOrderService {
    private final DailyCourseStatisticRepository repository;

    @Transactional
    public void processOrderCompleted(OrderCompletedMessage message) {
        // Chuyển Instant sang LocalDate
        LocalDate recordDate = message.getCompletedAt().atZone(ZoneId.systemDefault()).toLocalDate();

        for (OrderCompletedMessage.OrderItemDto item : message.getItems()) {
            BigDecimal finalPrice = item.getFinalPrice() != null ? item.getFinalPrice() : BigDecimal.ZERO;
            BigDecimal commissionRate = item.getCommissionRate() != null ? item.getCommissionRate() : BigDecimal.ZERO;

            // Tính toán phân chia doanh thu
            BigDecimal instructorRevenue = finalPrice.multiply(commissionRate);
            BigDecimal platformFee = finalPrice.subtract(instructorRevenue);

            // Tìm record của ngày hôm đó, nếu chưa có thì tạo mới
            DailyCourseStatistic stat = repository.findByCourseIdAndRecordDate(item.getCourseId(), recordDate)
                    .orElseGet(() -> DailyCourseStatistic.builder()
                            .courseId(item.getCourseId())
                            .instructorId(item.getInstructorId())
                            .recordDate(recordDate)
                            .totalOrderAmount(BigDecimal.ZERO)
                            .instructorRevenue(BigDecimal.ZERO)
                            .platformFee(BigDecimal.ZERO)
                            .dailyEnrollments(0)
                            .refundedAmount(BigDecimal.ZERO)
                            .refundedEnrollments(0)
                            .build());

            // Cộng dồn các chỉ số
            stat.setTotalOrderAmount(stat.getTotalOrderAmount().add(finalPrice));
            stat.setInstructorRevenue(stat.getInstructorRevenue().add(instructorRevenue));
            stat.setPlatformFee(stat.getPlatformFee().add(platformFee));
            stat.setDailyEnrollments(stat.getDailyEnrollments() + 1);

            repository.save(stat);
            log.info("💰 Đã cập nhật doanh thu khóa học {} - Tổng tiền: {}", item.getCourseId(), finalPrice);
        }
    }

}

