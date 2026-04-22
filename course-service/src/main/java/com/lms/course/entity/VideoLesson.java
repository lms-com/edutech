package com.lms.course.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "video_lessons")
@PrimaryKeyJoinColumn(name = "lesson_id") // Nối ID với bảng cha
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VideoLesson extends Lesson {

    @Column(name = "video_url", length = 500, nullable = false)
    private String videoUrl;

    @Column(nullable = false)
    private Integer duration;

    // Tự động set type khi lưu
    @Override
    @PrePersist
    public void prePersist() {
        super.prePersist();
        this.setType("VIDEO");
    }
}