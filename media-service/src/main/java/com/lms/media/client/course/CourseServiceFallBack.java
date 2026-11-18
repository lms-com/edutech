package com.lms.media.client.course;

import org.springframework.stereotype.Component;

@Component
public class CourseServiceFallBack implements CourseServiceFeignClient{

    @Override
    public boolean isEnrolled(String userId, String mediaId) {
        // Fail-closed: tuyệt đối không mở quyền xem video khi course-service gặp sự cố
        return false;
    }
}
