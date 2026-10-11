package com.lms.finance.client.feign.course;

import com.lms.common.dto.response.ApiResponse;
import com.lms.finance.client.feign.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(
        name = "course-service",
        configuration = FeignClientConfig.class
)
public interface CourseServiceFeignClient {

    @GetMapping("/api/internal/v1/instructors/{instructorId}/course-count")
    ApiResponse<Long> getInstructorCourseCount(@PathVariable("instructorId") String instructorId);

    @org.springframework.web.bind.annotation.PostMapping("/api/internal/v1/courses/bulk")
    List<com.lms.finance.client.feign.course.dto.CourseBulkResponse> getCourseBulk(@org.springframework.web.bind.annotation.RequestBody List<String> courseIds);
}
