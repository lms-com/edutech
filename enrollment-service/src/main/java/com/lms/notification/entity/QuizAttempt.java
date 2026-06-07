package com.lms.notification.entity;

import com.lms.common.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(name = "quiz_attempts")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuizAttempt extends AuditableEntity {

    @Id
    @Column(length = 36)
    String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", nullable = false)
    Enrollment enrollment;

    @Column(name = "lesson_id", nullable = false, length = 36)
    String lessonId; // ID của Quiz lesson trong Course Service

    @Column(name = "score", nullable = false)
    Integer score; // 0-100

    @Column(name = "is_passed", nullable = false)
    Boolean isPassed;

    @Column(name = "submitted_at", nullable = false)
    LocalDateTime submittedAt;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    Boolean isDeleted = false;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = java.util.UUID.randomUUID().toString();
        }
        if (submittedAt == null)
            submittedAt = LocalDateTime.now();
    }
}
