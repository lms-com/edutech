package com.lms.enrollment.dto.response;

import com.lms.enrollment.enums.EnrollmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentDetailResponse {
    private String id;
    private String courseId;
    private String learnerId;
    private EnrollmentStatus status;
    private LocalDateTime startedAt;
    private Integer completedRate;
    private List<LessonProgressResponse> progresses;
    private List<QuizAttemptResponse> quizAttempts;
}
