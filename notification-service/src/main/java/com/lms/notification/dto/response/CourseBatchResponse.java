package com.lms.notification.dto.response;

import lombok.Data;

@Data
public class CourseBatchResponse {
    private String courseId;
    private String title;
    private String slug;
    private String thumbnailUrl;
    private String instructorId;
    private String level;
    private String status;
}
