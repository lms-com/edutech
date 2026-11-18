package com.lms.notification.controller;

import com.lms.common.dto.response.ApiResponse;
import com.lms.common.swagger.annotation.RequireInternalKey;
import com.lms.notification.enums.NotificationType;
import com.lms.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller nội bộ dành cho các microservice khác (order-service, ...) gửi thông báo thông qua X-Internal-Key.
 */
@Hidden
@RestController
@RequestMapping("/api/internal/v1/notifications")
@RequireInternalKey
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InternalNotificationController {

    NotificationService notificationService;

    @PostMapping("/send-custom")
    public ApiResponse<Void> sendCustomNotification(
            @RequestParam String userId,
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam NotificationType type,
            @RequestParam(required = false) String refId,
            @RequestParam(required = false) String refType) {

        notificationService.createAndSendNotification(
                userId,
                title,
                content,
                type,
                refId,
                refType != null ? refType : "SYSTEM");
        return ApiResponse.success(null, "Sent internal notification successfully");
    }
}
