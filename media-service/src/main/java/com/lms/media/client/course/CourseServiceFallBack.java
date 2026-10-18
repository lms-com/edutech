package com.lms.media.client.course;

import org.springframework.stereotype.Component;

@Component
public class CourseServiceFallBack implements CourseServiceFeignClient{

    @Override
    public boolean isEnrolled(String userId, String mediaId) {
        return true;
    }
}
