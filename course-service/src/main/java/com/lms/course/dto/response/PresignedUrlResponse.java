package com.lms.course.dto.response;

import lombok.Data;

@Data
public class PresignedUrlResponse {
    private String uploadUrl;
    private String viewUrl;
}
