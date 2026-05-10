package com.lms.course.client;

import com.lms.common.dto.response.ApiResponse;
import com.lms.course.dto.request.PresignedUrlRequest;
import com.lms.course.dto.response.PresignedUrlResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "media-service", path = "/api/v1/media")
public interface MediaServiceClient {

    @PostMapping("/upload-url")
    ApiResponse<PresignedUrlResponse> getUploadUrl(@RequestBody PresignedUrlRequest request);

    @GetMapping("/view-url")
    ApiResponse<String> getViewUrl(@RequestParam("filePath") String filePath);
}
