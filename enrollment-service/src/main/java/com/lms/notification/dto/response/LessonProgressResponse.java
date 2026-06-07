package com.lms.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonProgressResponse {
    private String id;
    private String enrollmentId;
    private String lessonId;
    private Boolean isCompleted;
    private Integer lastWatchTimeSeconds;
}
