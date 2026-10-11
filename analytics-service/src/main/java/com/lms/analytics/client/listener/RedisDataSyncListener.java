package com.lms.analytics.client.listener;

import com.lms.analytics.dto.message.CourseSyncEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisDataSyncListener {

    private final RedisTemplate<String, Object> redisTemplate;

    // 1. Hứng Event Số dư từ finance-service
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "analytics.balance.sync.queue", durable = "true"),
            exchange = @Exchange(value = "finance.exchange", type = "direct"),
            key = "finance.balance.updated"
    ))
    public void onBalanceUpdated(Map<String, Object> message) {
        try {
            String instructorId = (String) message.get("instructorId");
            // Jackson map số sang kiểu Number, ta chuyển về String rồi parse cho an toàn
            String balanceStr = String.valueOf(message.get("availableBalance"));

            // Lưu vào Redis: Key là "analytics:balance:{instructorId}", Value là số dư
            redisTemplate.opsForValue().set("analytics:balance:" + instructorId, balanceStr);
            log.info("🔄 [Redis] Đã cập nhật số dư cho Giảng viên: {}", instructorId);
        } catch (Exception e) {
            log.error("❌ Lỗi khi đồng bộ số dư vào Redis", e);
        }
    }

    // 2. Hứng Event Khóa học từ course-service
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "analytics.course.sync.queue", durable = "true"),
            exchange = @Exchange(value = "course.exchange", type = "direct"),
            key = "course.info.updated"
    ))
    public void onCourseUpdated(CourseSyncEventMessage message) {
        try {
            String courseKey = "analytics:course:" + message.getCourseId();
            String instructorSetKey = "analytics:instructor_courses:" + message.getInstructorId();

            // 1. Luôn cập nhật Tên và Ảnh bìa
            redisTemplate.opsForHash().put(courseKey, "title", message.getTitle());
            redisTemplate.opsForHash().put(courseKey, "thumbnailUrl", message.getThumbnailUrl());

            // 2. Quyết định TĂNG hoặc GIẢM biến đếm
            if ("PUBLISHED".equals(message.getStatus()) && Boolean.FALSE.equals(message.getIsDeleted())) {
                redisTemplate.opsForSet().add(instructorSetKey, message.getCourseId());
            } else {
                redisTemplate.opsForSet().remove(instructorSetKey, message.getCourseId());
            }

            // Dòng Log bạn đã quên thêm:
            log.info("🔄 [Redis] Đã cập nhật thông tin khóa học: {}", message.getCourseId());

        } catch (Exception e) {
            log.error("❌ Lỗi khi đồng bộ khóa học vào Redis", e);
        }
    }

}
