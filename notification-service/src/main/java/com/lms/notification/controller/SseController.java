package com.lms.notification.controller;

import com.lms.notification.service.SseEmitterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class SseController {

    private final SseEmitterService sseEmitterService;

    // Frontend sẽ mở kết nối bằng Javascript: const eventSource = new EventSource('/api/v1/notifications/subscribe?userId=123');
    // BẮT BUỘC dùng produces = MediaType.TEXT_EVENT_STREAM_VALUE thì trình duyệt mới hiểu đây là luồng SSE
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@RequestParam String userId) {
        return sseEmitterService.subscribe(userId);
    }
}