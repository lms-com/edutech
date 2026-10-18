package com.lms.media.client.course;

import com.lms.media.client.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "course-service",
        configuration = FeignClientConfig.class,
        fallback = CourseServiceFallBack.class
)
public interface CourseServiceFeignClient {

    @GetMapping("/api/internal/v1/users/{userId}/videos/{mediaId}/access")
    public boolean isEnrolled (@PathVariable(name = "userId") String userId, @PathVariable(name = "mediaId") String mediaId);
}
