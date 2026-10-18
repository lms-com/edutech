package com.lms.course.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnswerResponse {
    private String id;
    private String optionText;
    // Thuộc tính này sẽ được truyền giá trị null (để @JsonInclude tự loại bỏ) hoặc giá trị boolean tùy thuộc vào việc API gọi cho Learner hay Admin/Instructor.
    private Boolean isCorrect;
}