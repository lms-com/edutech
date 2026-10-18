package com.lms.enrollment.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Phản hồi nội bộ đếm tổng số bài học từ course-service
 * (/api/internal/v1/courses/{courseId}/lesson-count).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonCountResponse {
    private String courseId;
    private long totalLessons;
}
