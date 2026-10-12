package com.lms.analytics.client.listener;

import com.lms.analytics.dto.message.CourseSyncEventMessage;
import com.lms.analytics.model.FailedSyncEvent;
import com.lms.analytics.repository.FailedSyncEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisDataSyncListener {

    private final RedisTemplate<String, Object> redisTemplate;
    private final FailedSyncEventRepository failedSyncEventRepository;

    // 1. Hứng Event Số dư từ finance-service
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(
                    value = "analytics.balance.sync.queue",
                    durable = "true",
                    arguments = {
                            @Argument(name = "x-dead-letter-exchange", value = "analytics.dlx"),
                            @Argument(name = "x-dead-letter-routing-key", value = "analytics.dlq.routing-key")
                    }
            ),
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
            value = @Queue(
                    value = "analytics.course.sync.queue",
                    durable = "true",
                    arguments = {
                            @Argument(name = "x-dead-letter-exchange", value = "analytics.dlx"),
                            @Argument(name = "x-dead-letter-routing-key", value = "analytics.dlq.routing-key")
                    }
            ),
            exchange = @Exchange(value = "course.exchange", type = "direct"),
            key = "course.info.updated"
    ))
    public void onCourseUpdated(CourseSyncEventMessage message) {
        if (true) throw new RuntimeException("Test DLQ: Cố tình giả lập lỗi Database sập!");
        try {
            String courseKey = "analytics:course:" + message.getCourseId();
            String instructorSetKey = "analytics:instructor_courses:" + message.getInstructorId();

            // 1. Luôn cập nhật Tên và Ảnh bìa
            redisTemplate.opsForHash().put(courseKey, "title", message.getTitle());
            redisTemplate.opsForHash().put(courseKey, "thumbnailUrl", message.getThumbnailUrl());

            // 2. Quyết định TĂNG hoặc GIẢM biến đếm
            if ("PUBLISHED".equals(message.getStatus()) && !Boolean.TRUE.equals(message.getIsDeleted())) {
                redisTemplate.opsForSet().add(instructorSetKey, message.getCourseId());
                redisTemplate.opsForSet().add("analytics:system_courses", message.getCourseId());
            } else {
                redisTemplate.opsForSet().remove(instructorSetKey, message.getCourseId());
                redisTemplate.opsForSet().remove("analytics:system_courses", message.getCourseId());
            }

            // Dòng Log bạn đã quên thêm:
            log.info("🔄 [Redis] Đã cập nhật thông tin khóa học: {}", message.getCourseId());

        } catch (Exception e) {
            log.error("❌ Lỗi khi đồng bộ khóa học vào Redis", e);
        }
    }

    // 3. Hứng Event Người dùng từ iam-service
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(
                    value = "analytics.user.sync.queue",
                    durable = "true",
                    arguments = {
                            @Argument(name = "x-dead-letter-exchange", value = "analytics.dlx"),
                            @Argument(name = "x-dead-letter-routing-key", value = "analytics.dlq.routing-key")
                    }
            ),
            exchange = @Exchange(value = "iam.exchange", type = "direct"),
            key = "iam.user.updated"
    ))
    public void onUserUpdated(Map<String, Object> message) {
        try {
            String userId = (String) message.get("userId");
            String status = (String) message.get("status");

            // Lưu Tên và Ảnh vào Redis
            String userKey = "analytics:user:" + userId;
            redisTemplate.opsForHash().put(userKey, "fullName", message.get("fullName"));
            redisTemplate.opsForHash().put(userKey, "avatarUrl", message.get("avatarUrl"));

            // Đếm User (Nếu status là ACTIVE thì mới đếm, tùy thuộc vào Enum Userstatus của cậu)
            if ("ACTIVE".equals(status)) {
                redisTemplate.opsForSet().add("analytics:system_users", userId);
            } else {
                redisTemplate.opsForSet().remove("analytics:system_users", userId);
            }

            log.info("🔄 [Redis] Đã cập nhật thông tin User: {}", userId);
        } catch (Exception e) {
            log.error("❌ Lỗi khi đồng bộ User vào Redis", e);
        }
    }

    // 4. Hứng Event lỗi (Dead Letter Queue)
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "analytics.dlq", durable = "true"),
            exchange = @Exchange(value = "analytics.dlx", type = "direct"),
            key = "analytics.dlq.routing-key"
    ))
    public void processFailedMessages(org.springframework.amqp.core.Message failedMessage) {
        try {
            String payload = new String(failedMessage.getBody());
            String exchangeName = failedMessage.getMessageProperties().getReceivedExchange();
            String routingKey = failedMessage.getMessageProperties().getReceivedRoutingKey();

            // Bóc tách lý do lỗi từ Header x-death do RabbitMQ ném kèm
            String errorReason = "Unknown Error";
            Map<String, Object> headers = failedMessage.getMessageProperties().getHeaders();
            if (headers.containsKey("x-death")) {
                errorReason = headers.get("x-death").toString();
            }

            log.error("🚨 [DLQ ALERT] Phát hiện Message lỗi! Đang lưu vào DB...");

            // Lưu vào Database
            FailedSyncEvent event = FailedSyncEvent.builder()
                    .exchangeName(exchangeName)
                    .routingKey(routingKey)
                    .payload(payload)
                    .errorReason(errorReason)
                    .status("FAILED")
                    .build();

            failedSyncEventRepository.save(event);
            log.info("✅ Đã lưu message lỗi vào bảng failed_sync_events thành công.");

        } catch (Exception e) {
            log.error("❌ Lỗi trầm trọng khi cố gắng lưu DLQ vào Database", e);
        }
    }

}
