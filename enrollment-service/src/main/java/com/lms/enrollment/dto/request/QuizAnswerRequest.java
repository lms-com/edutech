package com.lms.enrollment.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Lựa chọn của học viên cho một câu hỏi. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizAnswerRequest {

    @NotBlank(message = "Question id must not be blank")
    private String questionId;

    @NotBlank(message = "Answer id must not be blank")
    private String answerId;
}
