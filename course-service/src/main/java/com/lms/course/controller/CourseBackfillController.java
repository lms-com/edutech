package com.lms.course.controller;

import com.lms.course.entity.Course;
import com.lms.course.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/internal/courses")
@RequiredArgsConstructor
@Slf4j
public class CourseBackfillController {

    private final CourseRepository courseRepository;
    private final RabbitTemplate rabbitTemplate;

    @PostMapping("/sync-redis")
    public String syncCoursesToAnalytics() {
        int page = 0;
        int size = 500;
        int totalProcessed = 0;
        Page<Course> coursePage;

        do {
            coursePage = courseRepository.findAllNotDeleted(PageRequest.of(page, size));

            for (Course course : coursePage.getContent()) {
                if ("PUBLISHED".equals(course.getStatus())) {
                    Map<String, Object> message = Map.of(
                            "courseId", course.getId(),
                            "instructorId", course.getInstructorId(),
                            "title", course.getTitle(),
                            "thumbnailUrl", course.getThumbnailUrl()
                    );
                    rabbitTemplate.convertAndSend("course.exchange", "course.info.updated", message);
                    totalProcessed++;
                }
            }
            log.info("Đã đồng bộ batch {} của Course. Tổng: {}", page, totalProcessed);
            page++;

        } while (coursePage.hasNext());

        return "✅ Hoàn tất đồng bộ " + totalProcessed + " khóa học an toàn.";
    }
}
