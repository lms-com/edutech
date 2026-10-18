package com.lms.order.client.feign.enrollment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentValidationResponse {
    private Boolean hasAccess;
    private String enrollmentStatus;
}
