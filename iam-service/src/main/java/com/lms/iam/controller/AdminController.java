package com.lms.iam.controller;

import com.lms.common.dto.response.ApiResponse;
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
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;

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
}
