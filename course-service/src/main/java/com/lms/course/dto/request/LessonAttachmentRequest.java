package com.lms.course.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonAttachmentRequest {

    @NotBlank(message = "Tên tài liệu không được để trống")
    String name;

    @NotBlank(message = "Đường dẫn file không được để trống")
    String fileUrl;

    String fileSize;

    String fileType;
}
