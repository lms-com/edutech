package com.lms.iam.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Entity
@Table(name = "instructor_applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InstructorApplication {

    @Id
    @Column(name = "id", length = 36)
    String id;

    @Column(name = "user_id", length = 36, nullable = false)
    String userId;

    @Column(name = "bio", nullable = false, columnDefinition = "TEXT")
    String bio;

    @Column(name = "expertise", nullable = false, length = 255)
    String expertise;

    @Column(name = "experience_years")
    @Builder.Default
    Integer experienceYears = 0;

    @Column(name = "portfolio_url", length = 500)
    String portfolioUrl;

    @Column(name = "cv_url", length = 500)
    String cvUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    ApplicationStatus status = ApplicationStatus.PENDING;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    String rejectionReason;

    @Column(name = "reviewed_by", length = 36)
    String reviewedBy;

    @Column(name = "reviewed_at")
    Instant reviewedAt;

    @Column(name = "created_at", updatable = false)
    Instant createdAt;

    @Column(name = "updated_at")
    Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = java.util.UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
