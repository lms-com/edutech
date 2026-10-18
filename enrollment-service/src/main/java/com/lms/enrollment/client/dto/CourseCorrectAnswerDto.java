package com.lms.enrollment.client.dto;

import lombok.Data;

import java.util.List;

/**
 * Bản sao DTO CorrectAnswerResponse của course-service
 * (API nội bộ /api/internal/v1/lessons/{lessonId}/correct-answers).
 */
@Data
public class CourseCorrectAnswerDto {
    private String questionId;
    private String questionText;
    private List<String> correctAnswerIds;
    private String explanation;
    private Integer passScore;
}
