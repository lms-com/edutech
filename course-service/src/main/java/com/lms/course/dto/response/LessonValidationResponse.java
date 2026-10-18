package com.lms.course.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * API 38: Response nội bộ xác minh bài học có tồn tại và hợp lệ không.
 */
@Data
@Builder
public class LessonValidationResponse {
    private String lessonId;
    private String courseId;
    private String type; // VIDEO hoặc QUIZ
    private boolean valid; // true nếu lesson tồn tại và chưa bị xóa
}
