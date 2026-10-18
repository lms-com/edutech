package com.lms.course.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "video_lessons")
@PrimaryKeyJoinColumn(name = "lesson_id")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VideoLesson extends Lesson {

    @Column(name = "video_url")
    private String videoUrl;

    @Column(nullable = false)
    private Integer duration;

    @Override
    @PrePersist
    public void prePersist() {
        super.prePersist();
        this.setType("VIDEO"); // Tự động gán Type khi lưu
    }
}