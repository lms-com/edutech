package com.lms.enrollment.entity;

import com.lms.common.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Entity
@Table(name = "lesson_comments")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonComment extends AuditableEntity {

    @Id
    @Column(length = 36)
    String id;

    @Column(name = "lesson_id", nullable = false, length = 36)
    String lessonId;

    @Column(name = "course_id", nullable = false, length = 36)
    String courseId;

    @Column(name = "user_id", nullable = false, length = 36)
    String userId;

    @Column(name = "user_name")
    String userName;

    @Column(name = "user_avatar", length = 500)
    String userAvatar;

    @Column(name = "parent_id", length = 36)
    String parentId;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    String content;

    @Column(name = "likes_count", nullable = false)
    @Builder.Default
    Integer likesCount = 0;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    Boolean isDeleted = false;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (likesCount == null) {
            likesCount = 0;
        }
        if (isDeleted == null) {
            isDeleted = false;
        }
    }
}
