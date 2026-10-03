package com.lms.course.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class QuestionUpdateRequest {
    @NotBlank(message = "Nội dung câu hỏi không được để trống")
    private String questionText;

    @NotNull(message = "Danh sách đáp án không được để trống")
    @Valid
    private List<AnswerItemRequest> answers;
}