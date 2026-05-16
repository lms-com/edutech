package com.lms.media.controller;

import com.lms.media.service.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/")
@RequiredArgsConstructor
public class HLSController {

    private final MediaService mediaService;

    @GetMapping("/stream/{mediaId}/index.m3u8")
    public String getDynamicManifest (
            @PathVariable String mediaId,
            @RequestParam(name = "session") String sessionId
    ) {

        return "";
    }
}