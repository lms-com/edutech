package com.lms.analytics.service;

import com.lms.analytics.dto.response.AdminOverviewResponse;
import com.lms.analytics.dto.response.CoursePerformanceDto;
import com.lms.analytics.dto.response.InstructorPerformanceDto;

import java.util.List;

public interface AdminAnalyticsService {
    AdminOverviewResponse getAdminOverview();

    List<CoursePerformanceDto> getTop5CoursesSystemWide();

    List<InstructorPerformanceDto> getTop5InstructorsSystemWide();
}
