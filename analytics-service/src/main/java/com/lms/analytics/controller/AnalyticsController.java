package com.lms.analytics.controller;

import com.lms.analytics.dto.response.CoursePerformanceDto;
import com.lms.analytics.dto.response.DailyChartDto;
import com.lms.analytics.dto.response.InstructorOverviewResponse;
import com.lms.analytics.service.AnalyticsService;
import com.lms.common.dto.response.ApiResponse; // Nhớ import ApiResponse của bạn
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics/instructors")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    // API 1: Thẻ tổng quan
    @GetMapping("/{instructorId}/overview")
    public ApiResponse<InstructorOverviewResponse> getOverview(@PathVariable String instructorId) {
        InstructorOverviewResponse data = analyticsService.getInstructorOverview(instructorId);
        return ApiResponse.success(data);
    }

    // API 2: Biểu đồ (Mặc định lấy 30 ngày)
    @GetMapping("/{instructorId}/chart")
    public ApiResponse<List<DailyChartDto>> getChart(
            @PathVariable String instructorId,
            @RequestParam(defaultValue = "30") int days) {
        List<DailyChartDto> data = analyticsService.getInstructorRevenueChart(instructorId, days);
        return ApiResponse.success(data);
    }

    // API 3. Top 5 khóa học bán chạy
    @GetMapping("/{instructorId}/top-courses")
    public ApiResponse<List<CoursePerformanceDto>> getTopCourses(@PathVariable String instructorId) {
        List<CoursePerformanceDto> data = analyticsService.getTop5PerformingCourses(instructorId);
        return ApiResponse.success(data);
    }
}