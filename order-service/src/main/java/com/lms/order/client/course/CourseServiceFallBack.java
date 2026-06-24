package com.lms.order.client.course;

import com.lms.order.client.course.dto.CourseInternalDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class CourseServiceFallBack implements CourseServiceFeignClient {

    @Override
    public List<CourseInternalDto> getCoursesById(List<String> courseIds) {
        AtomicInteger count = new AtomicInteger(0);
        return courseIds.stream()
                .map(courseId -> CourseInternalDto.builder()
                                    .courseId(courseId)
                                    .courseName("😁 Course " + count.incrementAndGet() + " for Example")
                                    .currentPrice(new BigDecimal("99.90"))
                                    .currencyCode("USD")
                                    .instructorId("user-inst-01")
                                    .build()
                ).toList();
    }
}