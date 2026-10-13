package com.lms.iam.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubmitInstructorApplicationRequest {

    @NotBlank(message = "Tiểu sử và giới thiệu bản thân không được để trống")
    String bio;

    @NotBlank(message = "Lĩnh vực chuyên môn không được để trống")
    String expertise;

    Integer experienceYears;

    String portfolioUrl;

    String cvUrl;
}
