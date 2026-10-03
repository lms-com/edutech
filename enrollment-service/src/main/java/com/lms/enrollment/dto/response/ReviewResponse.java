package com.lms.enrollment.dto.response;

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
    /** Tên học viên, làm giàu từ iam-service; null nếu không lấy được. */
    private String learnerName;
    private String learnerAvatar;
    private Integer star;
    private String comment;
    private Instant createdAt;
}
