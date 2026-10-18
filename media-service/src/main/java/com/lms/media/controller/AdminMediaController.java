package com.lms.media.controller;

import com.lms.common.swagger.annotation.RequireJwt;
import com.lms.media.model.MediaFile;
import com.lms.media.service.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Media APIs for Admin")
@RequestMapping("/api/v1/admin")
@RequireJwt
@RequiredArgsConstructor
public class AdminMediaController {

    private final MediaService mediaService;

    @Operation(summary = "Get all media files")
    @GetMapping("/media")
    public List<MediaFile> getAllMediaFiles() {
        return mediaService.getAllMediaFiles();
    }
}
