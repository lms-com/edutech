package com.lms.enrollment.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizSubmitRequest {

    @NotNull(message = "Score must not be null")
    @Min(value = 0, message = "Score must be at least 0")
    @Max(value = 100, message = "Score must not exceed 100")
    private Integer score;
}
