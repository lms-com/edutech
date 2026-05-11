package com.lms.course.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SectionResponse {
    String id;
    String courseId;
    String title;
    Integer orderIndex;
}