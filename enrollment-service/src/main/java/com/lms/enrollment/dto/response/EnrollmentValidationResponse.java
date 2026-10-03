package com.lms.enrollment.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EnrollmentValidationResponse {
    Boolean hasAccess;
    String enrollmentStatus;  // "ACTIVE" | "REVOKED" | "NOT_FOUND"
}
