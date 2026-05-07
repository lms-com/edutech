package com.lms.course.entity;

import com.lms.common.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Category extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, updatable = false, nullable = false)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(name = "parent_id")
    private String parentId;

    // Cột mới thêm để phục vụ API số 6 (Sắp xếp)
    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(name = "is_deleted", nullable = false)
    private Boolean deleted;

    @PrePersist
    public void prePersist() {
        if (this.orderIndex == null) this.orderIndex = 0;
        if (this.deleted == null) this.deleted = false;
    }
}