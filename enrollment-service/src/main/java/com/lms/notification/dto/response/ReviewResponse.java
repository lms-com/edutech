package com.lms.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    private String id;
    private String enrollmentId;
    private String courseId;
    private String learnerId;
    private Integer star;
    private String comment;
    private Instant createdAt;
}
