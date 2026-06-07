package com.lms.notification.messaging;

import com.lms.notification.dto.event.CourseCompletedEvent;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EnrollmentPublisher {

    RabbitTemplate rabbitTemplate;

    public void publishCourseCompleted(CourseCompletedEvent event) {
        log.info("Publishing CourseCompletedEvent: {}", event);
        try {
            rabbitTemplate.convertAndSend("course-exchange", "course.completed", event);
        } catch (Exception e) {
            log.error("Failed to publish CourseCompletedEvent: {}", event, e);
        }
    }
}
