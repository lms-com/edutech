package com.lms.notification.entity;

import com.lms.common.model.AuditableEntity;
import com.lms.notification.enums.EnrollmentStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "enrollments", uniqueConstraints = @UniqueConstraint(name = "uk_enrollment_learner_course", columnNames = {
        "learner_id", "course_id" }))
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Enrollment extends AuditableEntity {

    @Id
    @Column(length = 36)
    String id;

    @Column(name = "learner_id", nullable = false, length = 36)
    String learnerId;

    @Column(name = "course_id", nullable = false, length = 36)
    String courseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    EnrollmentStatus status = EnrollmentStatus.ACTIVE;

    @Column(name = "started_at", nullable = false)
    LocalDateTime startedAt;

    // 0-100 — cập nhật mỗi khi học viên hoàn thành thêm 1 bài
    @Column(name = "completed_rate", nullable = false)
    @Builder.Default
    Integer completedRate = 0;

    @OneToMany(mappedBy = "enrollment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<LessonProgress> progresses = new ArrayList<>();

    @OneToMany(mappedBy = "enrollment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<QuizAttempt> quizAttempts = new ArrayList<>();

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    Boolean isDeleted = false;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = java.util.UUID.randomUUID().toString();
        }
        if (startedAt == null)
            startedAt = LocalDateTime.now();
    }
}
