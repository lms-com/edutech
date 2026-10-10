package com.lms.media.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GetUploadUrlRequest {
    String originalFileName;
    String contentType;
    Long fileSize;
}
