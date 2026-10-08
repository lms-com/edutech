package com.lms.enrollment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizResultResponse {
    private String lessonId;
    private Integer score;
    private Integer passScore;
    private Boolean isPassed;
    private String feedback;
    private List<QuestionResultDetail> details;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionResultDetail {
        private String questionId;
        private String questionText;
        private String selectedAnswerId;
        private List<String> correctAnswerIds;
        private Boolean isCorrect;
        private String explanation;
    }
}
