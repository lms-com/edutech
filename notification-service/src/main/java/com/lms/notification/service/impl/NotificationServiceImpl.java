package com.lms.notification.service.impl;

import com.lms.common.exception.AppException;
import com.lms.common.exception.CommonErrorCode;
import com.lms.notification.dto.response.NotificationResponse;
import com.lms.notification.dto.response.UnreadCountResponse;
import com.lms.notification.entity.Notification;
import com.lms.notification.enums.NotificationType;
import com.lms.notification.repository.NotificationRepository;
import com.lms.notification.service.NotificationService;
import com.lms.notification.service.SseEmitterService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class NotificationServiceImpl implements NotificationService {
    NotificationRepository notificationRepository;
    SseEmitterService sseEmitterService;

    // Hàm cốt lõi tạo thông báo, lưu DB và trigger đẩy SSE real-time
    @Override
    @Transactional
    public void createAndSendNotification(String userId, String title, String content,
                                          NotificationType type, String refId, String refType) {
        log.info("Creating notification for user: {}, title: {}", userId, title);

        Notification notification = Notification.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .title(title)
                .content(content)
                .type(type)
                .referenceId(refId)
                .referenceType(refType)
                .isRead(false)
                .build();

        notificationRepository.save(notification);

        try {
            NotificationResponse response = mapToResponse(notification);
            sseEmitterService.sendNotification(userId, response);
            log.info("Sent SSE notification to user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to send real-time notification via SSE to user: {}. Error: {}", userId, e.getMessage());
        }
    }

    // Xem danh sách thông báo của tôi (Có phân trang và bộ lọc isRead)
    @Override
    public Page<NotificationResponse> getMyNotifications(String userId, Boolean isRead, Pageable pageable) {
        log.info("Fetching notifications for user: {}, isRead: {}", userId, isRead);
        Page<Notification> notifications;
        if (isRead == null) {
            notifications = notificationRepository.findByUserId(userId, pageable);
        } else {
            notifications = notificationRepository.findByUserIdAndIsRead(userId, isRead, pageable);
        }
        return notifications.map(this::mapToResponse);
    }

    // Lấy số lượng thông báo chưa đọc
    @Override
    public UnreadCountResponse getUnreadCount(String userId) {
        log.info("Counting unread notifications for user: {}", userId);
        long count = notificationRepository.countByUserIdAndIsReadFalse(userId);
        return new UnreadCountResponse(count);
    }

    // Đánh dấu 1 thông báo đã đọc
    @Override
    @Transactional
    public void markAsRead(String notificationId, String userId) {
        log.info("Marking notification: {} as read for user: {}", notificationId, userId);
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new AppException(CommonErrorCode.UNAUTHORIZED, "Notification not found"));

        if (!notification.getUserId().equals(userId)) {
            throw new AppException(CommonErrorCode.UNAUTHORIZED, "You are not allowed to read this notification");
        }

        notification.setIsRead(true);
        notification.setReadAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    // Đánh dấu tất cả thông báo của tôi đã đọc
    @Override
    @Transactional
    public void markAllAsRead(String userId) {
        log.info("Marking all notifications as read for user: {}", userId);
        List<Notification> unreadNotifications = notificationRepository.findByUserIdAndIsReadFalse(userId);
        if (!unreadNotifications.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            unreadNotifications.forEach(notification -> {
                notification.setIsRead(true);
                notification.setReadAt(now);
            });
            notificationRepository.saveAll(unreadNotifications);
        }
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .content(notification.getContent())
                .isRead(notification.getIsRead())
                .readAt(notification.getReadAt())
                .referenceId(notification.getReferenceId())
                .referenceType(notification.getReferenceType())
                .createdAt(notification.getCreatedAt() != null ? java.time.LocalDateTime.ofInstant(notification.getCreatedAt(), java.time.ZoneId.systemDefault()) : null)
                .build();
    }
}
