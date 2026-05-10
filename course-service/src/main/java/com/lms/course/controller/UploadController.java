package com.lms.course.controller;

import com.lms.common.dto.response.ApiResponse;
import com.lms.course.client.MediaServiceClient;
import com.lms.course.dto.request.PresignedUrlRequest;
import com.lms.course.dto.response.PresignedUrlResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/uploads")
@RequiredArgsConstructor
@Tag(name = "Upload Controller", description = "Lấy URL Upload từ MinIO")
public class UploadController {

    private final MediaServiceClient mediaServiceClient;

    @Operation(summary = "33. Xin cấp URL Upload trực tiếp (Presigned Upload)", description = "Xin cấp URL Upload trực tiếp từ MinIO cho việc upload file ảnh/video.")
    @GetMapping("/presigned-url")
    // @PreAuthorize("hasAuthority('MEDIA_UPLOAD')")
    public ApiResponse<PresignedUrlResponse> getPresignedUrl(
            @RequestParam String filename,
            @RequestParam String contentType) {
        
        PresignedUrlRequest request = PresignedUrlRequest.builder()
                .filename(filename)
                .contentType(contentType)
                .build();
                
        // Gọi sang Media Service để lấy URL
        return mediaServiceClient.getUploadUrl(request);
    }
}