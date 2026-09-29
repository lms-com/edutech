package com.lms.course.dto.response;

import lombok.Data;

@Data
public class EnrollmentValidationResponse {
    private Boolean hasAccess;
    private String enrollmentStatus;
}
