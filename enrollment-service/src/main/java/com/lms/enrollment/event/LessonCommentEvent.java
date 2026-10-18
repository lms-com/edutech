package com.lms.enrollment.event;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonCommentEvent {
    String recipientId;
    String senderId;
    String senderName;
    String title;
    String content;
    String courseId;
    String lessonId;
    String commentId;
}
