package com.lms.notification.service;

import com.lms.notification.dto.response.NotificationResponse;
import com.lms.notification.dto.response.UnreadCountResponse;
import com.lms.notification.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {
    // Hàm cốt lõi tạo thông báo, lưu DB và trigger đẩy SSE real-time
    void createAndSendNotification(String userId, String title, String content,
                                   NotificationType type, String refId, String refType);

    // Xem danh sách thông báo của tôi (Có phân trang và bộ lọc isRead)
    Page<NotificationResponse> getMyNotifications(String userId, Boolean isRead, Pageable pageable);

    // Lấy số lượng thông báo chưa đọc
    UnreadCountResponse getUnreadCount(String userId);

    // Đánh dấu 1 thông báo đã đọc
    void markAsRead(String notificationId, String userId);

    // Đánh dấu tất cả thông báo của tôi đã đọc
    void markAllAsRead(String userId);
}