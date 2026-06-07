package com.lms.notification.entity;

import com.lms.common.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "lesson_progresses", uniqueConstraints = @UniqueConstraint(name = "uk_progress_enrollment_lesson", columnNames = {
        "enrollment_id", "lesson_id" }))
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonProgress extends AuditableEntity {

    @Id
    @Column(length = 36)
    String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", nullable = false)
    Enrollment enrollment;

    @Column(name = "lesson_id", nullable = false, length = 36)
    String lessonId;

    @Column(name = "is_completed", nullable = false)
    @Builder.Default
    Boolean isCompleted = false;

    // Giây cuối cùng học viên xem đến — flush từ Redis vào đây
    @Column(name = "last_watch_time_seconds", nullable = false)
    @Builder.Default
    Integer lastWatchTimeSeconds = 0;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    Boolean isDeleted = false;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = java.util.UUID.randomUUID().toString();
        }
    }
}
