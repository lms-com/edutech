package com.lms.course.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.course.dto.request.VideoStreamHeartbeatRequest;
import com.lms.course.dto.response.VideoStreamHeartbeatResponse;
import com.lms.course.service.VideoStreamSessionService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VideoStreamSessionServiceImpl implements VideoStreamSessionService {

    static final String REDIS_KEY_PREFIX = "video:stream:session:";
    static final Duration SESSION_TTL = Duration.ofSeconds(45);

    StringRedisTemplate redisTemplate;
    ObjectMapper objectMapper;

    @Override
    public VideoStreamHeartbeatResponse processHeartbeat(String lessonId, String userId, VideoStreamHeartbeatRequest request) {
        String key = REDIS_KEY_PREFIX + userId;
        String rawSession = redisTemplate.opsForValue().get(key);

        if (rawSession == null) {
            // Chưa có session nào đang giữ -> đăng ký session này
            saveSession(key, lessonId, request);
            return VideoStreamHeartbeatResponse.builder()
                    .active(true)
                    .conflict(false)
                    .message("Phiên xem hợp lệ")
                    .currentSessionId(request.getSessionId())
                    .build();
        }

        try {
            Map<?, ?> sessionData = objectMapper.readValue(rawSession, Map.class);
            String existingSessionId = (String) sessionData.get("sessionId");
            String clientPlatform = (String) sessionData.get("clientPlatform");

            if (request.getSessionId().equals(existingSessionId)) {
                // Khớp phiên -> refresh TTL
                redisTemplate.expire(key, SESSION_TTL);
                return VideoStreamHeartbeatResponse.builder()
                        .active(true)
                        .conflict(false)
                        .message("Phiên xem tiếp tục duy trì")
                        .currentSessionId(existingSessionId)
                        .build();
            } else {
                // Không khớp phiên -> Thiết bị khác đã chiếm session
                log.warn("⚠ [Concurrent Limiter] Phát hiện tài khoản [{}] bị xung đột xem đồng thời. Phiên cũ: [{}], Phiên đang chạy: [{}]",
                        userId, request.getSessionId(), existingSessionId);
                return VideoStreamHeartbeatResponse.builder()
                        .active(false)
                        .conflict(true)
                        .message("Tài khoản của bạn đang phát video trên một thiết bị hoặc phiên trình duyệt khác. Đã tạm dừng video để bảo vệ bản quyền.")
                        .currentSessionId(existingSessionId)
                        .activeDevice(clientPlatform != null ? clientPlatform : "Thiết bị khác")
                        .build();
            }
        } catch (Exception e) {
            log.error("Lỗi parse session video từ Redis cho user {}: {}", userId, e.getMessage());
            // Fallback: ghi đè session mới
            saveSession(key, lessonId, request);
            return VideoStreamHeartbeatResponse.builder()
                    .active(true)
                    .conflict(false)
                    .message("Khôi phục phiên xem thành công")
                    .currentSessionId(request.getSessionId())
                    .build();
        }
    }

    @Override
    public VideoStreamHeartbeatResponse claimSession(String lessonId, String userId, VideoStreamHeartbeatRequest request) {
        String key = REDIS_KEY_PREFIX + userId;
        saveSession(key, lessonId, request);
        log.info("✔ [Concurrent Limiter] Học viên [{}] đã xác nhận tiếp tục phát và chiếm quyền xem tại phiên [{}]",
                userId, request.getSessionId());

        return VideoStreamHeartbeatResponse.builder()
                .active(true)
                .conflict(false)
                .message("Đã chuyển quyền phát về thiết bị này")
                .currentSessionId(request.getSessionId())
                .build();
    }

    @Override
    public void releaseSession(String userId, String sessionId) {
        String key = REDIS_KEY_PREFIX + userId;
        String rawSession = redisTemplate.opsForValue().get(key);
        if (rawSession != null) {
            try {
                Map<?, ?> sessionData = objectMapper.readValue(rawSession, Map.class);
                String existingSessionId = (String) sessionData.get("sessionId");
                if (sessionId != null && sessionId.equals(existingSessionId)) {
                    redisTemplate.delete(key);
                    log.info("✔ [Concurrent Limiter] Đã giải phóng phiên xem cho user [{}]", userId);
                }
            } catch (Exception e) {
                redisTemplate.delete(key);
            }
        }
    }

    private void saveSession(String key, String lessonId, VideoStreamHeartbeatRequest request) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("sessionId", request.getSessionId());
            data.put("lessonId", lessonId);
            data.put("deviceFingerprint", request.getDeviceFingerprint());
            data.put("clientPlatform", request.getClientPlatform());
            data.put("updatedAt", System.currentTimeMillis());

            String json = objectMapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(key, json, SESSION_TTL);
        } catch (Exception e) {
            log.error("Lỗi lưu session video vào Redis: {}", e.getMessage());
        }
    }
}
