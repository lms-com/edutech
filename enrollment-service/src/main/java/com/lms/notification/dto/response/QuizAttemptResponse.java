package com.lms.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizAttemptResponse {
    private String id;
    private String enrollmentId;
    private String lessonId;
    private Integer score;
    private Boolean isPassed;
    private LocalDateTime submittedAt;
}
