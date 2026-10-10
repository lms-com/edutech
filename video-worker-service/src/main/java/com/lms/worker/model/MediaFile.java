package com.lms.worker.model;

import com.lms.common.model.AuditableEntity;
import com.lms.worker.model.MediaStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Table(name = "media_files")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MediaFile extends AuditableEntity {

    @Id
    String id;

    @Column(name = "original_file_name", nullable = false)
    String originalFileName;

    @Column(name = "stored_file_name", nullable = false)
    String storedFileName;

    @Column(name = "bucket_name", nullable = false)
    String bucketName;

    @Column(name = "content_type", nullable = false)
    String contentType;

    @Column(name = "file_size", nullable = false)
    Long fileSize;

    @Column(name = "status", nullable = false, columnDefinition = "varchar(255)")
    @Enumerated(EnumType.STRING)
    MediaStatus status;

    @Column(name = "hls_manifest_url")
    private String hlsManifestUrl;

    @Column(name = "encryption_key")
    private String encryptionKey;
}
