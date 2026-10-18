package com.lms.enrollment.dto.response;

import com.lms.enrollment.enums.EnrollmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentResponse {
    private String id;
    private String courseId;
    private String learnerId;
    private String learnerName;
    private String learnerAvatar;
    private EnrollmentStatus status;
    private LocalDateTime startedAt;
    private Integer completedRate;
}
