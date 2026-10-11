package com.lms.course.publisher;

import com.lms.course.dto.message.CourseSyncEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class CourseRabbitMQPublisher {

    private final RabbitTemplate rabbitTemplate;

    // Chìa khóa là AFTER_COMMIT: Đợi MySQL lưu xong xuôi mới chạy hàm này
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCoursePublishedEvent(CourseSyncEventMessage event) {
        try {
            Map<String, Object> message = Map.of(
                    "courseId", event.getCourseId(),
                    "instructorId", event.getInstructorId(),
                    "title", event.getTitle(),
                    "thumbnailUrl", event.getThumbnailUrl()
            );

            rabbitTemplate.convertAndSend("course.exchange", "course.info.updated", message);
            log.info("📤 Đã bắn Event đồng bộ khóa học {} sang Analytics.", event.getCourseId());
        } catch (Exception e) {
            log.error("❌ Lỗi khi bắn Event đồng bộ khóa học", e);
        }
    }
}
