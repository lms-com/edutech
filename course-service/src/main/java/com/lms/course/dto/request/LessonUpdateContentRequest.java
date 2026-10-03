package com.lms.course.dto.request;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import java.math.BigDecimal;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonUpdateContentRequest {
    String title;
    Boolean freePreview;

    // Dành cho Video Lesson
    String videoUrl;
    Integer duration;

    // Dành cho Quiz Lesson
    BigDecimal passScore;
}