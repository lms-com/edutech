package com.lms.iam.controller;

import com.lms.common.dto.response.ApiResponse;
import com.lms.common.exception.AppException;
import com.lms.common.swagger.annotation.RequireJwt;
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
@RequireJwt
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

    private final com.lms.iam.service.InstructorApplicationService applicationService;

    @Operation(summary = "Nộp đơn đăng ký làm giảng viên", description = "Học viên gửi thông tin chuyên môn để xin xét duyệt làm giảng viên")
    @org.springframework.web.bind.annotation.PostMapping("/me/instructor-applications")
    public ApiResponse<com.lms.iam.dto.response.InstructorApplicationResponse> submitInstructorApplication(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody com.lms.iam.dto.request.SubmitInstructorApplicationRequest request
    ) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is null");
        }
        com.lms.iam.dto.response.InstructorApplicationResponse result = applicationService.submitApplication(userId, request);
        return ApiResponse.success(result, "Nộp đơn đăng ký làm giảng viên thành công. Vui lòng chờ quản trị viên phê duyệt.");
    }

    @Operation(summary = "Xem lịch sử đơn đăng ký giảng viên của tôi", description = "Kiểm tra trạng thái các đơn đăng ký giảng viên đã nộp")
    @GetMapping("/me/instructor-applications")
    public ApiResponse<List<com.lms.iam.dto.response.InstructorApplicationResponse>> getMyInstructorApplications(
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is null");
        }
        return ApiResponse.success(applicationService.getMyApplications(userId), "Lấy danh sách đơn đăng ký thành công");
    }
}
