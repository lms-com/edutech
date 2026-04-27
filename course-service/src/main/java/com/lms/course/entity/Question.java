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
@Builder
public class Question extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, updatable = false, nullable = false)
    private String id;

    // Liên kết với Quiz. Chú ý: Cột lưu trữ ID là quiz_id, trỏ về ID của Quiz (chính là lesson_id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(name = "question_text", columnDefinition = "TEXT", nullable = false)
    private String questionText;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @PrePersist
    public void prePersist() {
        if (this.orderIndex == null) {
            this.orderIndex = 0;
        }
    }
}