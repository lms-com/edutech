package com.lms.notification.event.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseCompletedEvent {
    private String enrollmentId;
    private String learnerId;
    private String courseId;
    private Instant completedAt;
}
