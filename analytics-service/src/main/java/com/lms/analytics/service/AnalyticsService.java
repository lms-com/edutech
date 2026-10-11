package com.lms.analytics.service;

import com.lms.analytics.dto.response.CoursePerformanceDto;
import com.lms.analytics.dto.response.DailyChartDto;
import com.lms.analytics.dto.response.InstructorOverviewResponse;

import java.util.List;

public interface AnalyticsService {

    InstructorOverviewResponse getInstructorOverview (String instructorId);

    List<CoursePerformanceDto> getTop5PerformingCourses(String instructorId);

    List<DailyChartDto> getInstructorRevenueChart (String instructorId, int days);
}
