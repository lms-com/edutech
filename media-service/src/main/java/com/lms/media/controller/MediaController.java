package com.lms.media.controller;

import com.lms.common.dto.response.ApiResponse;
import com.lms.media.client.course.CourseServiceFeignClient;
import com.lms.media.dto.request.GetUploadUrlRequest;
import com.lms.media.dto.response.GetUploadUrlResponse;
import com.lms.media.service.MediaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
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

    @PostMapping("/{mediaId}/confirm")
    public ApiResponse<Void> confirm (@PathVariable("mediaId") String mediaId){
        mediaService.confirmUploadUrl(mediaId);
        return ApiResponse.success(null, "Confirmed successfully!");
    }

    @GetMapping("/{mediaId}/view")
    public ApiResponse<?> getDisplayUrl (@PathVariable String mediaId){
        return ApiResponse.success(null/*mediaService.getDisplayUrl(mediaId)*/);
    }

    private final CourseServiceFeignClient courseServiceFeignClient;

    @GetMapping("/test/tao/lao")
    public ApiResponse<?> getTaoLao () {
        if (courseServiceFeignClient.isEnrolled("user123", "media321")) {
            log.info("😅");
        }
        return ApiResponse.success(null, "😁😁😁 Get Internal Key successfully!");
    }
}
