package com.lms.notification.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.lms.common.dto.response.ApiResponse;
import com.lms.notification.dto.response.CourseBatchResponse;

@FeignClient(name = "course-service")
public interface CourseServiceClient {
    @PostMapping("/api/internal/v1/courses/batch")
    ApiResponse<List<CourseBatchResponse>> getCourseBatch(@RequestBody List<String> courseIds);
}
