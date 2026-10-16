package com.lms.enrollment.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonCommentResponse {

    String id;
    String lessonId;
    String courseId;
    String userId;
    String userName;
    String userAvatar;
    String parentId;
    String content;
    Integer likesCount;
    Instant createdAt;
    Instant updatedAt;

    // Danh sách câu trả lời lồng nhau (replies)
    List<LessonCommentResponse> replies;
}
