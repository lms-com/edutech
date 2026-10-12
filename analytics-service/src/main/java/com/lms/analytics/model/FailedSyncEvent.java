package com.lms.analytics.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "failed_sync_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FailedSyncEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(name = "exchange_name")
    String exchangeName;

    @Column(name = "routing_key")
    String routingKey;

    @Column(name = "payload", columnDefinition = "TEXT", nullable = false)
    String payload;

    @Column(name = "error_reason", columnDefinition = "TEXT")
    String errorReason;

    @Column(name = "status", length = 50)
    String status; // FAILED, RESOLVED

    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = "FAILED";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}