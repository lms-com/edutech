package com.lms.finance.service;

import com.lms.finance.dto.response.InstructorAnalyticsOverviewResponse;
import com.lms.finance.dto.response.RevenueChartPointResponse;

import java.util.List;

public interface InstructorAnalyticsService {

    /**
     * Lấy tổng quan các chỉ số KPI doanh thu, số học viên, số khóa học và số dư ví của giảng viên.
     */
    InstructorAnalyticsOverviewResponse getOverview(String instructorId);

    /**
     * Lấy dữ liệu biểu đồ doanh thu theo 6 tháng gần nhất của giảng viên.
     */
    List<RevenueChartPointResponse> getRevenueChart(String instructorId);

    /**
     * Lấy thống kê chi tiết doanh thu theo từng khóa học (doanh thu gộp, phí sàn, thực nhận, số lượt bán).
     */
    List<com.lms.finance.dto.response.CourseRevenueBreakdownResponse> getCourseBreakdown(String instructorId);
}
