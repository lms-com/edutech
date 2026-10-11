package com.lms.analytics.service.impl;

import com.lms.analytics.dto.response.CoursePerformanceDto;
import com.lms.analytics.dto.response.DailyChartDto;
import com.lms.analytics.dto.response.InstructorOverviewResponse;
import com.lms.analytics.repository.DailyCourseStatisticRepository;
import com.lms.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {
    private final DailyCourseStatisticRepository repository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public InstructorOverviewResponse getInstructorOverview(String instructorId) {
        // 1. Tính toán từ MySQL (Doanh thu & Học viên)
        YearMonth currentMonth = YearMonth.now();
        LocalDate startOfMonth = currentMonth.atDay(1);
        LocalDate endOfMonth = currentMonth.atEndOfMonth();

        BigDecimal revenueThisMonth = repository.sumInstructorRevenueByDateRange(instructorId, startOfMonth, endOfMonth);
        Integer totalEnrollments = repository.sumTotalEnrollmentsByInstructor(instructorId);

        // 2. Lấy dữ liệu từ Redis
        // Lấy Số dư khả dụng
        Object balanceObj = redisTemplate.opsForValue().get("analytics:balance:" + instructorId);
        BigDecimal availableBalance = balanceObj != null ? new BigDecimal(balanceObj.toString()) : BigDecimal.ZERO;

        // Lấy Tổng số lượng khóa học của Giảng viên (Đếm số phần tử trong Set)
        Long totalCoursesLong = redisTemplate.opsForSet().size("analytics:instructor_courses:" + instructorId);
        Integer totalCourses = totalCoursesLong != null ? totalCoursesLong.intValue() : 0;

        // Rating tạm thời để 0.0 hoặc 5.0 (Vì cần có hệ thống Review-service bắn event qua mới có)
        Double averageRating = 5.0;

        return InstructorOverviewResponse.builder()
                .availableBalance(availableBalance)
                .totalRevenueThisMonth(revenueThisMonth)
                .totalEnrollments(totalEnrollments)
                .totalCourses(totalCourses)
                .averageRating(averageRating)
                .build();
    }

    @Override
    public List<CoursePerformanceDto> getTop5PerformingCourses(String instructorId) {
        // 1. Lấy danh sách ID và Doanh thu từ MySQL
        List<CoursePerformanceDto> topCourses = repository.findTopCoursesByInstructor(
                instructorId,
                PageRequest.of(0, 5)
        );

        // 2. Lấy Tên và Ảnh bìa từ Redis đắp vào
        for (CoursePerformanceDto dto : topCourses) {
            String courseKey = "analytics:course:" + dto.getCourseId();

            Object titleObj = redisTemplate.opsForHash().get(courseKey, "title");
            Object thumbObj = redisTemplate.opsForHash().get(courseKey, "thumbnailUrl");

            dto.setCourseName(titleObj != null ? titleObj.toString() : "Khóa học không xác định");
            dto.setThumbnailUrl(thumbObj != null ? thumbObj.toString() : "");
        }

        return topCourses;
    }

    @Override
    public List<DailyChartDto> getInstructorRevenueChart(String instructorId, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);
        return repository.getChartDataByInstructor(instructorId, startDate, endDate);
    }
}
