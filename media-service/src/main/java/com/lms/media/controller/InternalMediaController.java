package com.lms.media.controller;

import com.lms.common.dto.response.ApiResponse;
import com.lms.media.service.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/v1")
@RequiredArgsConstructor
public class InternalMediaController {

    private final MediaService mediaService;

    @DeleteMapping("/{mediaId}")
    public ApiResponse<?> deleteFile (@PathVariable String mediaId) {
        mediaService.removeFile(mediaId);
        return ApiResponse.success(
                null,
                "Delete file Successfully!"
        );
    }
}
