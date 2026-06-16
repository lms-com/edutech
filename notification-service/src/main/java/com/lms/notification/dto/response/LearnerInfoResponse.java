package com.lms.notification.dto.response;

import lombok.Data;

@Data
public class LearnerInfoResponse {
    private String id;
    private String fullName;
    private String email;
    private String avatarUrl;
}
