package com.lms.course.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * API 39: Response nội bộ cấp danh sách đáp án đúng cho Enrollment Service chấm điểm tự động.
 */
@Data
@Builder
public class CorrectAnswerResponse {
    private String questionId;
    private String questionText;
    private List<String> correctAnswerIds; // Danh sách ID đáp án đúng
}
