package com.lms.analytics.dto.message;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CourseSyncEventMessage {
    private String courseId;
    private String instructorId;
    private String title;
    private String thumbnailUrl;
    private String status;     // "DRAFT", "PUBLISHED", "UNPUBLISHED", v.v.
    private Boolean isDeleted; // true/false
}
