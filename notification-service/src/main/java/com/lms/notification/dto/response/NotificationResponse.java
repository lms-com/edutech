package com.lms.notification.dto.response;

import com.lms.notification.enums.NotificationType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationResponse {
    String id;
    NotificationType type;
    String title;
    String content;
    Boolean isRead;
    LocalDateTime readAt;
    String referenceId;
    String referenceType;
    LocalDateTime createdAt;

}
