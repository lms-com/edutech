package com.lms.media.controller;

import com.lms.media.model.MediaFile;
import com.lms.media.service.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminMediaController {

    private final MediaService mediaService;

    @GetMapping("/media")
    public List<MediaFile> getAllMediaFiles() {
        return mediaService.getAllMediaFiles();
    }
}
