package com.lms.iam.controller;

import com.lms.common.dto.response.ApiResponse;
import com.lms.common.exception.AppException;
import com.lms.iam.dto.response.UserProfileReponse;
import com.lms.iam.exception.IamErrorCode;
import com.lms.iam.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserProfileReponse> getMyProfile (@RequestHeader(value = "X-User-Id", required = false) String userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is null");
        }
        return ApiResponse.success(userService.getUserProfile(userId));
    }
}
