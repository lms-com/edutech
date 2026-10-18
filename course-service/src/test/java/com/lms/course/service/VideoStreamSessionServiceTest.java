package com.lms.course.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.course.dto.request.VideoStreamHeartbeatRequest;
import com.lms.course.dto.response.VideoStreamHeartbeatResponse;
import com.lms.course.service.impl.VideoStreamSessionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VideoStreamSessionServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private ObjectMapper objectMapper;
    private VideoStreamSessionServiceImpl sessionService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        sessionService = new VideoStreamSessionServiceImpl(redisTemplate, objectMapper);
    }

    @Test
    @DisplayName("Heartbeat lần đầu khi chưa có session -> Tạo mới thành công và active = true")
    void testHeartbeat_NewSession() {
        when(valueOperations.get(anyString())).thenReturn(null);

        VideoStreamHeartbeatRequest request = VideoStreamHeartbeatRequest.builder()
                .sessionId("sess-123")
                .deviceFingerprint("fp-123")
                .clientPlatform("Chrome")
                .build();

        VideoStreamHeartbeatResponse response = sessionService.processHeartbeat("lesson-1", "user-1", request);

        assertTrue(response.isActive());
        assertFalse(response.isConflict());
        assertEquals("sess-123", response.getCurrentSessionId());
        verify(valueOperations).set(eq("video:stream:session:user-1"), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("Heartbeat cùng sessionId hiện tại -> Refresh TTL và active = true")
    void testHeartbeat_SameSession() {
        String existingJson = "{\"sessionId\":\"sess-123\",\"lessonId\":\"lesson-1\",\"clientPlatform\":\"Chrome\"}";
        when(valueOperations.get("video:stream:session:user-1")).thenReturn(existingJson);

        VideoStreamHeartbeatRequest request = VideoStreamHeartbeatRequest.builder()
                .sessionId("sess-123")
                .deviceFingerprint("fp-123")
                .clientPlatform("Chrome")
                .build();

        VideoStreamHeartbeatResponse response = sessionService.processHeartbeat("lesson-1", "user-1", request);

        assertTrue(response.isActive());
        assertFalse(response.isConflict());
        verify(redisTemplate).expire(eq("video:stream:session:user-1"), any(Duration.class));
    }

    @Test
    @DisplayName("Heartbeat khác sessionId -> Phát hiện xung đột concurrent stream và conflict = true")
    void testHeartbeat_ConflictSession() {
        String existingJson = "{\"sessionId\":\"sess-DEVICE-B\",\"lessonId\":\"lesson-1\",\"clientPlatform\":\"Thiết bị thứ hai\"}";
        when(valueOperations.get("video:stream:session:user-1")).thenReturn(existingJson);

        VideoStreamHeartbeatRequest request = VideoStreamHeartbeatRequest.builder()
                .sessionId("sess-DEVICE-A")
                .deviceFingerprint("fp-device-a")
                .clientPlatform("Thiết bị thứ nhất")
                .build();

        VideoStreamHeartbeatResponse response = sessionService.processHeartbeat("lesson-1", "user-1", request);

        assertFalse(response.isActive());
        assertTrue(response.isConflict());
        assertEquals("sess-DEVICE-B", response.getCurrentSessionId());
        assertEquals("Thiết bị thứ hai", response.getActiveDevice());
    }

    @Test
    @DisplayName("Claim session -> Ghi đè thành công và active = true")
    void testClaimSession() {
        VideoStreamHeartbeatRequest request = VideoStreamHeartbeatRequest.builder()
                .sessionId("sess-NEW")
                .deviceFingerprint("fp-new")
                .clientPlatform("Mobile")
                .build();

        VideoStreamHeartbeatResponse response = sessionService.claimSession("lesson-1", "user-1", request);

        assertTrue(response.isActive());
        assertFalse(response.isConflict());
        assertEquals("sess-NEW", response.getCurrentSessionId());
        verify(valueOperations).set(eq("video:stream:session:user-1"), anyString(), any(Duration.class));
    }
}
