package com.lms.finance.service.impl;

import com.lms.common.dto.response.ApiResponse;
import com.lms.finance.client.feign.course.CourseServiceFeignClient;
import com.lms.finance.dto.response.InstructorAnalyticsOverviewResponse;
import com.lms.finance.dto.response.RevenueChartPointResponse;
import com.lms.finance.entity.InstructorBalance;
import com.lms.finance.entity.RevenueShare;
import com.lms.finance.repository.InstructorBalanceRepository;
import com.lms.finance.repository.RevenueShareRepository;
import com.lms.finance.service.InstructorAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class InstructorAnalyticsServiceImpl implements InstructorAnalyticsService {

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final RevenueShareRepository revenueShareRepository;
    private final InstructorBalanceRepository balanceRepository;
    private final CourseServiceFeignClient courseServiceFeignClient;

    @Override
    @Transactional(readOnly = true)
    public InstructorAnalyticsOverviewResponse getOverview(String instructorId) {
        log.info("📊 Generating analytics overview for instructor: {}", instructorId);

        YearMonth currentMonth = YearMonth.now(VIETNAM_ZONE);
        Instant startOfCurrentMonth = currentMonth.atDay(1).atStartOfDay(VIETNAM_ZONE).toInstant();
        Instant startOfNextMonth = currentMonth.plusMonths(1).atDay(1).atStartOfDay(VIETNAM_ZONE).toInstant();

        YearMonth lastMonth = currentMonth.minusMonths(1);
        Instant startOfLastMonth = lastMonth.atDay(1).atStartOfDay(VIETNAM_ZONE).toInstant();

        // 1. Thống kê doanh thu từ RevenueShare
        BigDecimal thisMonthRevenue = revenueShareRepository.sumInstructorAmountByInstructorIdAndCreatedAtBetween(
                instructorId, startOfCurrentMonth, startOfNextMonth);
        if (thisMonthRevenue == null) thisMonthRevenue = BigDecimal.ZERO;

        BigDecimal lastMonthRevenue = revenueShareRepository.sumInstructorAmountByInstructorIdAndCreatedAtBetween(
                instructorId, startOfLastMonth, startOfCurrentMonth);
        if (lastMonthRevenue == null) lastMonthRevenue = BigDecimal.ZERO;

        long thisMonthSales = revenueShareRepository.countByInstructorIdAndCreatedAtBetween(
                instructorId, startOfCurrentMonth, startOfNextMonth);

        BigDecimal allTimeRevenue = revenueShareRepository.sumInstructorAmountByInstructorId(instructorId);
        if (allTimeRevenue == null) allTimeRevenue = BigDecimal.ZERO;

        long totalStudents = revenueShareRepository.countDistinctOrdersByInstructorId(instructorId);

        // 2. Tính tỷ lệ tăng trưởng so với tháng trước
        double growthRate = 0.0;
        if (lastMonthRevenue.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal diff = thisMonthRevenue.subtract(lastMonthRevenue);
            growthRate = diff.divide(lastMonthRevenue, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(1, RoundingMode.HALF_UP)
                    .doubleValue();
        } else if (thisMonthRevenue.compareTo(BigDecimal.ZERO) > 0) {
            growthRate = 100.0;
        }

        // 3. Số dư ví thực tế từ InstructorBalance
        InstructorBalance balance = balanceRepository.findByInstructorId(instructorId).orElse(null);
        BigDecimal availableBalance = balance != null ? balance.getAvailableBalance() : BigDecimal.ZERO;
        BigDecimal pendingBalance = balance != null ? balance.getPendingBalance() : BigDecimal.ZERO;
        BigDecimal actualBalance = balance != null ? balance.getActualBalance() : BigDecimal.ZERO;

        // 4. Lấy tổng số khóa học từ Course Service (kèm fallback an toàn)
        long totalCourses = 0;
        try {
            ApiResponse<Long> courseCountRes = courseServiceFeignClient.getInstructorCourseCount(instructorId);
            if (courseCountRes != null && courseCountRes.getData() != null) {
                totalCourses = courseCountRes.getData();
            }
        } catch (Exception e) {
            log.warn("⚠️ Không thể lấy số lượng khóa học từ course-service cho instructor {}: {}", instructorId, e.getMessage());
            totalCourses = revenueShareRepository.findByInstructorIdAndCreatedAtGreaterThanEqualOrderByCreatedAtAsc(
                    instructorId, Instant.EPOCH).stream()
                    .map(RevenueShare::getCourseId)
                    .distinct()
                    .count();
        }

        return InstructorAnalyticsOverviewResponse.builder()
                .totalStudents(totalStudents)
                .totalCourses(totalCourses)
                .thisMonthRevenue(thisMonthRevenue)
                .lastMonthRevenue(lastMonthRevenue)
                .growthRate(growthRate)
                .thisMonthSales(thisMonthSales)
                .availableBalance(availableBalance)
                .pendingBalance(pendingBalance)
                .actualBalance(actualBalance)
                .allTimeRevenue(allTimeRevenue)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RevenueChartPointResponse> getRevenueChart(String instructorId) {
        log.info("📈 Generating revenue trend chart (last 6 months) for instructor: {}", instructorId);

        YearMonth currentMonth = YearMonth.now(VIETNAM_ZONE);
        YearMonth sixMonthsAgo = currentMonth.minusMonths(5);
        Instant startOfRange = sixMonthsAgo.atDay(1).atStartOfDay(VIETNAM_ZONE).toInstant();

        // Chuẩn bị danh sách 6 tháng liên tiếp (kể cả tháng chưa có doanh thu)
        Map<YearMonth, RevenueChartPointResponse> chartMap = new LinkedHashMap<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = currentMonth.minusMonths(i);
            String period = ym.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            String label = String.format("T%02d/%d", ym.getMonthValue(), ym.getYear());
            chartMap.put(ym, RevenueChartPointResponse.builder()
                    .period(period)
                    .label(label)
                    .revenue(BigDecimal.ZERO)
                    .grossSales(BigDecimal.ZERO)
                    .orderCount(0L)
                    .build());
        }

        // Lấy tất cả RevenueShare từ thời điểm 6 tháng trước
        List<RevenueShare> shares = revenueShareRepository
                .findByInstructorIdAndCreatedAtGreaterThanEqualOrderByCreatedAtAsc(instructorId, startOfRange);

        for (RevenueShare share : shares) {
            if (share.getCreatedAt() == null) continue;
            YearMonth ym = YearMonth.from(share.getCreatedAt().atZone(VIETNAM_ZONE));
            RevenueChartPointResponse point = chartMap.get(ym);
            if (point != null) {
                BigDecimal instructorAmt = share.getInstructorAmount() != null ? share.getInstructorAmount() : BigDecimal.ZERO;
                BigDecimal grossAmt = share.getGrossAmount() != null ? share.getGrossAmount() : BigDecimal.ZERO;
                point.setRevenue(point.getRevenue().add(instructorAmt));
                point.setGrossSales(point.getGrossSales().add(grossAmt));
                point.setOrderCount(point.getOrderCount() + 1);
            }
        }

        return new ArrayList<>(chartMap.values());
    }
}
