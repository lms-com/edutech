package com.lms.media.controller;

import com.lms.common.dto.response.ApiResponse;
import com.lms.media.client.course.CourseServiceFeignClient;
import com.lms.media.dto.request.GetUploadUrlRequest;
import com.lms.media.dto.response.GetUploadUrlResponse;
import com.lms.media.service.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@Tag(name = "Media Controller", description = "APIs for working with video")
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    @Operation(summary = "Get video presigned upload url")
    @PostMapping("/upload-url")
    public ApiResponse<GetUploadUrlResponse> getUploadUrl (@RequestBody GetUploadUrlRequest request){
        return ApiResponse.success(
                mediaService.requestUploadUrl(request)
        );
    }

    @Operation(summary = "Confirm video")
    @PostMapping("/{mediaId}/confirm")
    public ApiResponse<Void> confirm (@PathVariable("mediaId") String mediaId){
        mediaService.confirmUploadUrl(mediaId);
        return ApiResponse.success(null, "Confirmed successfully!");
    }

    @Operation(summary = "Get manifest content", description = "Get manifest content contains paths of .ts files and URL for requesting encryption key in order to decrypt .ts files")
    @GetMapping("/{mediaId}/view")
    public ResponseEntity<String> getVideoManifest (@RequestHeader(name = "X-User-Id") String learnerId, @PathVariable String mediaId){

        String dynamicManifest = mediaService.getVideoManifest(learnerId, mediaId);

        // BẮT BUỘC trả về Content-Type là mpegurl thì trình duyệt mới phát được video
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "application/vnd.apple.mpegurl")
                .body(dynamicManifest);
    }


    private final CourseServiceFeignClient courseServiceFeignClient;

    @GetMapping("/test/tao/lao")
    public ApiResponse<?> getTaoLao () {
        if (courseServiceFeignClient.isEnrolled("user123", "media321")) {
            log.info("😅");
        }
        return ApiResponse.success(null, "😁😁😁 Get Internal Key successfully!");
    }

    @GetMapping("/secure/key/{mediaId}")
    public ResponseEntity<byte[]> getEncryptionKey (
                @RequestHeader("X-User-Id") String learnerId,
                @PathVariable("mediaId") String mediaId,
                @RequestParam("session") String sessionId
    ) {
        byte[] encryptionKey = mediaService.getEncryptionKey(learnerId, mediaId, sessionId);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(encryptionKey);
    }
}
