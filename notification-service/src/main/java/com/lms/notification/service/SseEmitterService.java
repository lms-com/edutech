package com.lms.notification.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface SseEmitterService {
    // Hàm dành cho Frontend gọi lên để thiết lập đường ống kết nối giữ lâu (Long-lived connection)
    SseEmitter subscribe(String userId);

    // Hàm dành cho các Service nội bộ gọi để đẩy thông báo real-time xuống cho 1 User cụ thể
    void sendNotification(String userId, Object payload);
}