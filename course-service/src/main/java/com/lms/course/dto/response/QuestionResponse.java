package com.lms.course.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class QuestionResponse {
    private String id;
    private String questionText;
    private Integer orderIndex;
    private List<AnswerResponse> answers;
}