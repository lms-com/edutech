package com.lms.course.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "quizzes")
@PrimaryKeyJoinColumn(name = "lesson_id") // Nối ID với bảng cha
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Quiz extends Lesson {

    @Column(name = "pass_score", precision = 5, scale = 2, nullable = false)
    private BigDecimal passScore;

    // Tự động set type khi lưu
    // Tự động set type khi lưu
    @Override
    @PrePersist
    public void prePersist() {
        super.prePersist();
        this.setType("VIDEO");
    }
}