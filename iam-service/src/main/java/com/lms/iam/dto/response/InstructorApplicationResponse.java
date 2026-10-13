package com.lms.iam.dto.response;

import com.lms.iam.model.ApplicationStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InstructorApplicationResponse {

    String id;
    String userId;
    String applicantName;
    String applicantEmail;
    String bio;
    String expertise;
    Integer experienceYears;
    String portfolioUrl;
    String cvUrl;
    ApplicationStatus status;
    String rejectionReason;
    String reviewedBy;
    Instant reviewedAt;
    Instant createdAt;
    Instant updatedAt;
}
