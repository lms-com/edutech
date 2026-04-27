package com.lms.course.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonResponse {
    String id;
    String title;
    String type;
    Integer orderIndex;
    Boolean freePreview;

    // Các trường trả về phụ thuộc vào Type
    String videoUrl;
    Integer duration;
    BigDecimal passScore;
}