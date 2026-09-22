package com.lms.notification.entity;

import com.lms.common.model.AuditableEntity;
import com.lms.notification.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "notifications",
        indexes = {
                @Index(name = "idx_notifications_user",      columnList = "user_id, created_at DESC"),
                @Index(name = "idx_notifications_user_read", columnList = "user_id, is_read")
        }
)
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Notification extends AuditableEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    String id; // Khóa chính tường minh giải quyết triệt để lỗi IDE báo đỏ

    @Column(name = "user_id", nullable = false, length = 36)
    String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50, columnDefinition = "VARCHAR(50)")
    NotificationType type;

    @Column(name = "title", nullable = false)
    String title;

    @Column(name = "content", columnDefinition = "TEXT")
    String content;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    Boolean isRead = false;

    @Column(name = "read_at")
    LocalDateTime readAt;

    @Column(name = "reference_id", length = 36)
    String referenceId;

    @Column(name = "reference_type", length = 50)
    String referenceType;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = java.util.UUID.randomUUID().toString(); // Tự động sinh UUID v4 khi insert
        }
    }
}