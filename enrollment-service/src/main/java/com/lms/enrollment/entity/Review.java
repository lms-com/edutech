package com.lms.enrollment.entity;

import com.lms.common.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(
    name = "reviews",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_review_enrollment",
        columnNames = {"enrollment_id"}
    )
)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Review extends AuditableEntity {

    @Id
    @Column(length = 36)
    String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", nullable = false)
    Enrollment enrollment;

    // Denormalized — lưu thẳng để query theo courseId không cần JOIN
    @Column(name = "course_id", nullable = false, length = 36)
    String courseId;

    @Column(name = "star", nullable = false)
    Integer star;   // 1-5

    @Column(name = "comment", columnDefinition = "TEXT")
    String comment;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = java.util.UUID.randomUUID().toString();
        }
    }
}
