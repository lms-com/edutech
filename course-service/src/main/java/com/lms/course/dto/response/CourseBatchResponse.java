package com.lms.course.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * API 36: Response nội bộ cấp thông tin cơ bản nhiều khóa học cùng lúc.
 */
@Data
@Builder
public class CourseBatchResponse {
    private String courseId;
    private String title;
    private String slug;
    private String thumbnailUrl;
    private String instructorId;
    private String level;
    private String status;
}
