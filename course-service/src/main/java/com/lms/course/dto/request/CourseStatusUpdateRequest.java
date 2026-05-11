package com.lms.course.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CourseStatusUpdateRequest {
    @NotBlank(message = "Trạng thái không được để trống")
    private String status;
}
