package com.lms.course.dto.request;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonCreateRequest {
    // Thông tin chung
    String sectionId;
    String title;
    String type; // Truyền vào "VIDEO" hoặc "QUIZ"
    Integer orderIndex;
    Boolean freePreview;

    // Dành riêng cho VIDEO
    String videoUrl;
    Integer duration;

    // Dành riêng cho QUIZ
    BigDecimal passScore;
}