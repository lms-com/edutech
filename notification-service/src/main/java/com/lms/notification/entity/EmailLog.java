package com.lms.notification.entity;

import com.lms.common.model.AuditableEntity;
import com.lms.notification.enums.EmailStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "email_logs",
        indexes = {
                @Index(name = "idx_email_logs_recipient", columnList = "recipient_email, created_at DESC"),
                @Index(name = "idx_email_logs_status",    columnList = "status")
        }
)
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EmailLog extends AuditableEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    String id;

    @Column(name = "recipient_email", nullable = false)
    String recipientEmail;

    @Column(name = "subject", nullable = false, length = 500)
    String subject;

    @Column(name = "template_name", nullable = false, length = 100)
    String templateName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    @Builder.Default
    EmailStatus status = EmailStatus.SENT;

    @Column(name = "error_message", columnDefinition = "TEXT")
    String errorMessage;

    @Column(name = "sent_at")
    LocalDateTime sentAt;

    @Column(name = "reference_id", length = 36)
    String referenceId;

    @Column(name = "reference_type", length = 50)
    String referenceType;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = java.util.UUID.randomUUID().toString();
        }
    }
}