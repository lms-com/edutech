package com.lms.iam.controller;

import com.lms.common.dto.response.ApiResponse;
import com.lms.common.swagger.annotation.RequireJwt;
import com.lms.iam.dto.request.UpdateUserStatusRequest;
import com.lms.iam.dto.response.UserResponse;
import com.lms.iam.model.User;
import com.lms.iam.model.Userstatus;
import com.lms.iam.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Admin Controller", description = "API related to system, operations and user administration")
@RequestMapping("/api/v1/admin")
@RequireJwt
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final com.lms.iam.service.DeviceManagementService deviceService;

    @Operation(summary = "Get all users")
    @GetMapping("/users")
    @PreAuthorize("USER_MANAGE")
    public ApiResponse<Page<UserResponse>> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Userstatus status,
            @RequestParam(required = false) String roleName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
            )
    {
        Page<UserResponse> users = userService.getAllUsers(search, status, roleName, page, size);
        return ApiResponse.<Page<UserResponse>>builder()
                .code(200)
                .message("User list retrieved successfully")
                .data(users)
                .build();
    }


    @Operation(summary = "Lock/Unlock User Account", description = "Change User status and API access of account")
    @PutMapping("/users/{userId}/status")
    public ApiResponse<?> updateUserStatus(
            @PathVariable(name = "userId", required = true) String userId,
            @RequestBody UpdateUserStatusRequest request)
    {
        userService.updateUserStatus(userId, request);
        return ApiResponse.success("User status updated successfully");
    }

    @Operation(summary = "Liệt kê tất cả thiết bị phiên đăng nhập", description = "Lấy danh sách thiết bị đang hoạt động trên hệ thống lưu trong Redis")
    @GetMapping("/devices")
    public ApiResponse<List<com.lms.iam.dto.response.AdminDeviceResponse>> getAllActiveDevices(
            @RequestParam(required = false) String search
    ) {
        List<com.lms.iam.dto.response.AdminDeviceResponse> devices = deviceService.getAllActiveDevices(search);
        return ApiResponse.success(devices, "Lấy danh sách thiết bị thành công");
    }

    @Operation(summary = "Thu hồi phiên thiết bị", description = "Xóa thiết bị khỏi Redis và đưa vào danh sách đen")
    @PostMapping("/devices/revoke")
    public ApiResponse<String> revokeDevice(
            @RequestParam String userId,
            @RequestParam String deviceFingerprint
    ) {
        deviceService.revokeDevice(userId, deviceFingerprint);
        return ApiResponse.success("Đã thu hồi phiên và chặn thiết bị thành công");
    }

    private final com.lms.iam.service.InstructorApplicationService applicationService;

    @Operation(summary = "Danh sách đơn đăng ký giảng viên", description = "Lấy danh sách đơn đăng ký giảng viên của học viên")
    @GetMapping("/instructor-applications")
    public ApiResponse<Page<com.lms.iam.dto.response.InstructorApplicationResponse>> getInstructorApplications(
            @RequestParam(required = false) com.lms.iam.model.ApplicationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        Page<com.lms.iam.dto.response.InstructorApplicationResponse> result = applicationService.getApplicationsForAdmin(status, pageable);
        return ApiResponse.success(result, "Lấy danh sách đơn đăng ký giảng viên thành công");
    }

    @Operation(summary = "Xét duyệt đơn đăng ký giảng viên", description = "Phê duyệt hoặc từ chối đơn đăng ký làm giảng viên")
    @PutMapping("/instructor-applications/{applicationId}/review")
    public ApiResponse<com.lms.iam.dto.response.InstructorApplicationResponse> reviewInstructorApplication(
            @RequestHeader(value = "X-User-Id", required = false) String adminId,
            @PathVariable String applicationId,
            @RequestBody com.lms.iam.dto.request.ReviewInstructorApplicationRequest request
    ) {
        String reviewer = (adminId != null && !adminId.isBlank()) ? adminId : "system-admin";
        com.lms.iam.dto.response.InstructorApplicationResponse result = applicationService.reviewApplication(reviewer, applicationId, request);
        return ApiResponse.success(result, Boolean.TRUE.equals(request.getApproved()) ? "Đã phê duyệt đơn và cấp quyền Giảng viên thành công" : "Đã từ chối đơn đăng ký giảng viên");
    }
}
