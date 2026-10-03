package com.lms.media.controller;

import com.lms.common.dto.response.ApiResponse;
import com.lms.media.service.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/v1")
@RequiredArgsConstructor
@Tag(name = "Internal Media Controller", description = "APIs serve internal services")
public class InternalMediaController {

    private final MediaService mediaService;

    @Operation(summary = "Delete media file", description = "Delete media file from database and minio")
    @DeleteMapping("media-jobs/lessons/{lessonId}/files")
    public ApiResponse<?> deleteFile (@PathVariable(name = "lessonId") String mediaId) {
        mediaService.removeFile(mediaId);
        return ApiResponse.success(
                null,
                "Delete file Successfully!"
        );
    }
}
