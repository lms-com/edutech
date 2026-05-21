package com.lms.order.client.course;

import com.lms.order.client.FeignClientConfig;
import com.lms.order.client.course.dto.CourseInternalDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "course-service",
        configuration = FeignClientConfig.class,
        fallback = CourseServiceFallBack.class
)
public interface CourseServiceFeignClient {

    // Lay info cua Course tai thoi diem hien tai:
    @GetMapping("/api/internal/v1/courses/{courseId}")
    CourseInternalDto getCourseById (@PathVariable String courseId);
}
