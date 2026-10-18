package com.lms.course.entity;

import com.lms.common.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lessons")
@Inheritance(strategy = InheritanceType.JOINED) // Kỹ thuật tách bảng cha - con
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class Lesson extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, updatable = false, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 20)
    private String type; // VIDEO, QUIZ, DOC

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    // Đã sửa: Không dùng tiền tố "is" để Lombok gen hàm đúng chuẩn
    @Column(name = "is_free_preview")
    private Boolean freePreview;

    // Bắt buộc có cho Soft Delete
    @Column(name = "is_deleted", nullable = false)
    private Boolean deleted;

// Đã bổ sung annotation
    @PrePersist
    public void prePersist() {
        if (this.orderIndex == null) this.orderIndex = 0;
        if (this.freePreview == null) this.freePreview = false;
        if (this.deleted == null) this.deleted = false;
    }
}