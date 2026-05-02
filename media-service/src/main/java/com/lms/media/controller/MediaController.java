package com.lms.media.controller;

import com.lms.common.dto.response.ApiResponse;
import com.lms.media.dto.request.GetUploadUrlRequest;
import com.lms.media.dto.response.GetUploadUrlResponse;
import com.lms.media.service.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    @PostMapping("/upload-url")
    public ApiResponse<GetUploadUrlResponse> getUploadUrl (@RequestBody GetUploadUrlRequest request){
        return ApiResponse.success(
                mediaService.requestUploadUrl(request)
        );
    }
}
