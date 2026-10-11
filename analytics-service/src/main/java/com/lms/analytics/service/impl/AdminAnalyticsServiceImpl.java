package com.lms.analytics.service.impl;

import com.lms.analytics.dto.response.AdminOverviewResponse;
import com.lms.analytics.dto.response.CoursePerformanceDto;
import com.lms.analytics.dto.response.InstructorPerformanceDto;
import com.lms.analytics.repository.DailyCourseStatisticRepository;
import com.lms.analytics.service.AdminAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminAnalyticsServiceImpl implements AdminAnalyticsService {

    private final DailyCourseStatisticRepository repository;
    private final RedisTemplate<String, Object> redisTemplate;
    // Tạm thời chưa gọi tới Redis hoặc FeignClient cho tổng User/Course.

    @Override
    public AdminOverviewResponse getAdminOverview() {
        YearMonth currentMonth = YearMonth.now();
        LocalDate startOfMonth = currentMonth.atDay(1);
        LocalDate endOfMonth = currentMonth.atEndOfMonth();

        BigDecimal gmv = repository.sumPlatformGmvByDateRange(startOfMonth, endOfMonth);
        BigDecimal netRevenue = repository.sumPlatformNetRevenueByDateRange(startOfMonth, endOfMonth);

        // TODO: Chờ User-Service/Course-Service hoàn thiện API hoặc đẩy Redis để lấy data này
        Integer totalUsers = 0;
        Integer totalCourses = 0;

        return AdminOverviewResponse.builder()
                .totalGmvThisMonth(gmv)
                .netRevenueThisMonth(netRevenue)
                .totalUsers(totalUsers)
                .totalCourses(totalCourses)
                .build();
    }

    @Override
    public List<CoursePerformanceDto> getTop5CoursesSystemWide() {
        List<CoursePerformanceDto> topCourses = repository.findTopCoursesSystemWide(PageRequest.of(0, 5));

        // Tận dụng luôn data trên Redis để đắp vào
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
    public List<InstructorPerformanceDto> getTop5InstructorsSystemWide() {
        List<InstructorPerformanceDto> topInstructors = repository.findTopInstructorsSystemWide(PageRequest.of(0, 5));
        // TODO: Chặng tới lấy Tên/Avatar Giảng viên từ Redis đắp vào
        return topInstructors;
    }
}
