package com.lms.course.entity;

import com.lms.common.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Question extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, updatable = false, nullable = false)
    private String id;

    // Liên kết với Quiz (chính là Lesson ID)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

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