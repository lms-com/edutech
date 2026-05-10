package com.lms.course.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SectionCreateRequest {

    @NotNull(message = "Course ID không được để trống")
    String courseId;

    @NotBlank(message = "Tên chương học không được để trống")
    String title;

    Integer orderIndex;
}