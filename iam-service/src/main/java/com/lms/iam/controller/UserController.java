package com.lms.iam.controller;

import com.lms.common.dto.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    @GetMapping("/me")
    public ApiResponse<String> getMyProfile (@RequestHeader(value = "X-User-Id", required = false) String userId) {
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setCode(200);
        apiResponse.setMessage("Success");
        apiResponse.setData("Chào mừng bạn đã vượt qua Gateway! ID của bạn là: " + userId);
        return apiResponse;
    }
}
