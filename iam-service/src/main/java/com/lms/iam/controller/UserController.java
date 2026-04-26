package com.lms.iam.controller;

import com.lms.common.dto.response.ApiResponse;
import com.lms.common.exception.AppException;
import com.lms.iam.dto.response.UserDeviceResponse;
import com.lms.iam.dto.response.UserProfileReponse;
import com.lms.iam.exception.IamErrorCode;
import com.lms.iam.service.DeviceManagementService;
import com.lms.iam.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "User Controller", description = "Personal API for User's deserving")
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final DeviceManagementService deviceService;

    @Operation(summary = "Get my profile", description = "Get personal informations")
    @GetMapping("/me")
    public ApiResponse<UserProfileReponse> getMyProfile (@RequestHeader(value = "X-User-Id", required = false) String userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is null");
        }
        return ApiResponse.success(userService.getUserProfile(userId));
    }

    @Operation(summary = "Get my devices", description = "Get the devices currently logged in")
    @GetMapping("/me/devices")
    public ApiResponse<List<UserDeviceResponse>> getMyDevices (@RequestHeader(value = "X-User-Id", required = false) String userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is null");
        }
        return ApiResponse.success(
                deviceService.getUserDevices(userId),
                "Get user's devices successfully"
        );
    }
}
