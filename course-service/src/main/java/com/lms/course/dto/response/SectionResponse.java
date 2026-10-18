package com.lms.course.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SectionResponse {
    String id;
    String courseId;
    String title;
    Integer orderIndex;

    // Danh sách bài học thuộc chương này (dùng cho API 8 - Course Detail)
    List<LessonResponse> lessons;
}