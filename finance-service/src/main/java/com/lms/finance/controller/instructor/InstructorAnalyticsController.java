package com.lms.finance.controller.instructor;

import com.lms.common.dto.response.ApiResponse;
import com.lms.common.swagger.annotation.RequireJwt;
import com.lms.finance.dto.response.InstructorAnalyticsOverviewResponse;
import com.lms.finance.dto.response.RevenueChartPointResponse;
import com.lms.finance.service.InstructorAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Instructor Analytics Controller", description = "APIs thống kê và biểu đồ phân tích doanh thu dành cho Giảng viên")
@RestController
@RequestMapping("/api/v1/instructor/analytics")
@RequireJwt
@RequiredArgsConstructor
public class InstructorAnalyticsController {

    private final InstructorAnalyticsService analyticsService;

    @Operation(summary = "Lấy tổng quan KPI giảng viên (Học viên, Doanh thu, Khóa học, Số dư)")
    @GetMapping("/overview")
    public ApiResponse<InstructorAnalyticsOverviewResponse> getOverview(
            @RequestHeader(value = "X-User-Id") String instructorId) {
        return ApiResponse.success(analyticsService.getOverview(instructorId));
    }

    @Operation(summary = "Lấy dữ liệu biểu đồ doanh thu theo 6 tháng gần nhất")
    @GetMapping("/revenue-chart")
    public ApiResponse<List<RevenueChartPointResponse>> getRevenueChart(
            @RequestHeader(value = "X-User-Id") String instructorId) {
        return ApiResponse.success(analyticsService.getRevenueChart(instructorId));
    }
}
