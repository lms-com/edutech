package com.lms.course.dto.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PresignedUrlRequest {
    private String filename;
    private String contentType;
}
