package com.lms.enrollment.entity;

import com.lms.common.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "reviews", uniqueConstraints = @UniqueConstraint(name = "uk_review_enrollment", columnNames = {
        "enrollment_id" }))
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    // Số sao đánh giá từ 1 đến 5 sao
    // Star rating from 1 to 5 stars
    @Column(name = "star", nullable = false, columnDefinition = "TINYINT")
    Integer star; // 1-5

    @Column(name = "comment", columnDefinition = "TEXT")
    String comment;

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
