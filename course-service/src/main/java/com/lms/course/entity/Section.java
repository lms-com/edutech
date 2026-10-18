package com.lms.course.entity;

import com.lms.common.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Section extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, updatable = false, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false)
    private String title;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(name = "is_deleted", nullable = false)
    private Boolean deleted;

    @OneToMany(mappedBy = "section", fetch = FetchType.LAZY)
    @org.hibernate.annotations.Where(clause = "is_deleted = false")
    @OrderBy("orderIndex ASC")
    private java.util.List<Lesson> lessons;

    @PrePersist
    public void prePersist() {
        if (this.orderIndex == null) this.orderIndex = 0;
        if (this.deleted == null) this.deleted = false;
    }

}
