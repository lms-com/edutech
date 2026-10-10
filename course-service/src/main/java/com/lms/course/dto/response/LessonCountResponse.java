package com.lms.course.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * API 37: Response nội bộ đếm tổng bài học cho Enrollment Service.
 */
@Data
@Builder
public class LessonCountResponse {
    private String courseId;
    private long totalLessons;
}
