package com.lms.course.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuestionResponse {
    private String id;
    private String questionText;
    private String explanation;
    private Integer orderIndex;
    private List<AnswerResponse> answers;
}