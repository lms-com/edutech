package com.lms.course.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CourseRejectRequest {
    @NotBlank(message = "Lý do từ chối không được để trống")
    private String rejectionNote;
}
