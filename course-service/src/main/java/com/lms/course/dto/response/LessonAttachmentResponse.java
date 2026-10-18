package com.lms.course.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonAttachmentResponse {

    String id;
    String lessonId;
    String name;
    String fileUrl;
    String fileSize;
    String fileType;
    Instant createdAt;
}
