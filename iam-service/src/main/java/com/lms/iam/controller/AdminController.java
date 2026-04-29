package com.lms.iam.controller;

import com.lms.common.dto.response.ApiResponse;
import com.lms.iam.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Admin Controller", description = "API related to system, operations and user administration")
@RequestMapping("/api/v1/admin")
public class AdminController {

    @Operation(summary = "Get all users")
    @GetMapping("/users")
    public ApiResponse<?> getAllUsers() {
        return ApiResponse.builder()
                .code(10001)
                .message("Get all users successfully")
                .build();
    }
}
