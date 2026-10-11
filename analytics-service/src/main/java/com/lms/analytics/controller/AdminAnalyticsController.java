package com.lms.analytics.controller;

import com.lms.analytics.dto.response.AdminOverviewResponse;
import com.lms.analytics.dto.response.CoursePerformanceDto;
import com.lms.analytics.dto.response.InstructorPerformanceDto;
import com.lms.analytics.service.AdminAnalyticsService;
import com.lms.common.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics/admin")
@RequiredArgsConstructor
public class AdminAnalyticsController {

    private final AdminAnalyticsService adminAnalyticsService;

    // 1. Thẻ tổng quan cho Sàn
    @GetMapping("/overview")
    public ApiResponse<AdminOverviewResponse> getOverview() {
        return ApiResponse.success(adminAnalyticsService.getAdminOverview());
    }

    // 2. Top 5 khóa học toàn sàn
    @GetMapping("/top-courses")
    public ApiResponse<List<CoursePerformanceDto>> getTopCourses() {
        return ApiResponse.success(adminAnalyticsService.getTop5CoursesSystemWide());
    }

    // 3. Top 5 giảng viên toàn sàn
    @GetMapping("/top-instructors")
    public ApiResponse<List<InstructorPerformanceDto>> getTopInstructors() {
        return ApiResponse.success(adminAnalyticsService.getTop5InstructorsSystemWide());
    }
}