package com.lms.order.client.feign.course;

import com.lms.order.client.feign.FeignClientConfig;
import com.lms.order.client.feign.course.dto.CourseInternalRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(
        name = "course-service",
        configuration = FeignClientConfig.class
        //, fallback = CourseServiceFallBack.class
)
public interface CourseServiceFeignClient {

    // Lay info cua Course tai thoi diem hien tai:
    @PostMapping("/api/internal/v1/courses/bulk")
    List<CourseInternalRequest> getCoursesById (@RequestBody List<String> courseId);
}