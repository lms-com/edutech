package com.lms.notification.controller;

import com.lms.common.dto.response.ApiResponse;
import com.lms.notification.dto.response.NotificationResponse;
import com.lms.notification.dto.response.UnreadCountResponse;
import com.lms.notification.service.NotificationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationController {

    NotificationService notificationService;

    @GetMapping("/me")
    public ApiResponse<Page<NotificationResponse>> getMyNotification(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<NotificationResponse> notifications = notificationService.getMyNotifications(userId, isRead, pageable);
        return ApiResponse.success(notifications);
    }

    @GetMapping("/unread-count")
    public ApiResponse<UnreadCountResponse> getUnreadCount(
            @RequestHeader("X-User-Id") String userId) {
        return ApiResponse.success(notificationService.getUnreadCount(userId));
    }

    @PutMapping("/{notificationId}/read")
    public ApiResponse<Void> markAsRead(
            @PathVariable String notificationId,
            @RequestHeader("X-User-Id") String userId) {
        notificationService.markAsRead(notificationId, userId);
        return ApiResponse.success(null, "Marked notification as read successfully");
    }

    @PutMapping("/read-all")
    public ApiResponse<Void> markAllAsRead(
            @RequestHeader("X-User-Id") String userId) {
        notificationService.markAllAsRead(userId);
        return ApiResponse.success(null, "Marked all notifications as read successfully");
    }

    // API tạm thời để test đẩy thông báo real-time
    @PostMapping("/simulate-trigger")
    public ApiResponse<Void> simulateTrigger(
            @RequestParam String userId,
            @RequestParam String title,
            @RequestParam String content) {

        notificationService.createAndSendNotification(
                userId,
                title,
                content,
                com.lms.notification.enums.NotificationType.ORDER_COMPLETED,
                "test-ref-123",
                "ORDER");
        return ApiResponse.success(null, "Triggered simulated notification successfully");
    }

}
