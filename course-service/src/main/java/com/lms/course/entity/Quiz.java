package com.lms.course.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "quizzes")
@PrimaryKeyJoinColumn(name = "lesson_id")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Quiz extends Lesson {

    @Column(name = "pass_score", precision = 5, scale = 2, nullable = false)
    private BigDecimal passScore;

    @Column(name = "duration", nullable = false)
    private Integer duration;

    @Override
    @PrePersist
    public void prePersist() {
        super.prePersist();
        this.setType("QUIZ"); // Tự động gán Type khi lưu
        if (this.duration == null || this.duration <= 0) {
            this.duration = 900; // Mặc định 15 phút (900 giây)
        }
    }
}