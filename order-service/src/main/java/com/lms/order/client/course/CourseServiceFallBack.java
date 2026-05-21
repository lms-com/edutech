package com.lms.order.client.course;

import com.lms.order.client.course.dto.CourseInternalDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class CourseServiceFallBack implements CourseServiceFeignClient {

    @Override
    public CourseInternalDto getCourseById(String courseId) {
        return CourseInternalDto.builder()
                .courseId(courseId)
                .courseName("😁 Course Name for Example")
                .currentPrice(new BigDecimal("1999000.00"))
                .currencyCode("VND")
                .instructorId("user-inst-01")
                .build();
    }
}
