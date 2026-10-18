package com.lms.notification.entity;

import com.lms.common.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "certificates",
        uniqueConstraints = {
                // Employer quét QR → query bằng hash này
                @UniqueConstraint(name = "uk_certificate_qr_hash",    columnNames = {"qr_code_hash"}),
                // Chống cấp chứng chỉ 2 lần cho cùng enrollment
                @UniqueConstraint(name = "uk_certificate_enrollment", columnNames = {"enrollment_id"})
        },
        indexes = {
                @Index(name = "idx_certificate_learner", columnList = "learner_id"),
                @Index(name = "idx_certificate_course", columnList = "course_id")
        }
)
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Certificate extends AuditableEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    String id; // Bổ sung khóa chính tường minh để tránh lỗi IDE / Compiler báo đỏ

    @Column(name = "learner_id", nullable = false, length = 36)
    String learnerId;

    @Column(name = "course_id", nullable = false, length = 36)
    String courseId;

    @Column(name = "enrollment_id", nullable = false, length = 36)
    String enrollmentId;

    // UUID random → encode Base64 URL-safe → nhúng vào QR Code
    // Employer quét QR → GET /certificates/verify/{qrCodeHash}
    @Column(name = "qr_code_hash", nullable = false, length = 100)
    String qrCodeHash;

    // Đường dẫn file PDF trên MinIO sau khi upload
    @Column(name = "pdf_url", nullable = false, length = 500)
    String pdfUrl;

    @Column(name = "issued_at", nullable = false)
    LocalDateTime issuedAt;

    @PrePersist
    void prePersist() {
        if (this.id == null) {
            this.id = java.util.UUID.randomUUID().toString(); // Tự động sinh ID
        }
        if (this.issuedAt == null) {
            this.issuedAt = LocalDateTime.now();
        }
    }
}