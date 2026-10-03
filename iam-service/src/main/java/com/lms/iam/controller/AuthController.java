package com.lms.iam.controller;

import com.lms.common.dto.response.ApiResponse;
import com.lms.iam.dto.request.LoginRequest;
import com.lms.iam.dto.request.LogoutRequest;
import com.lms.iam.dto.request.RegisterRequest;
import com.lms.iam.dto.response.LoginResponse;
import com.lms.iam.dto.response.RegisterResponse;
import com.lms.iam.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth Controller", description = "API related to authentication and authorization")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Login", description = "Login with email and password")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Login request", required = true)
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody @Valid LoginRequest loginRequest) {
        LoginResponse loginResponse = authService.login(loginRequest);
        return ApiResponse.success(loginResponse, "Login successfully");
    }

    @Operation(summary = "Register", description = "Register with email and password")
    @PostMapping("/register")
    public ApiResponse<RegisterResponse> register(@RequestBody @Valid RegisterRequest registerRequest) {
        RegisterResponse registerData = authService.register(registerRequest);
        return ApiResponse.success(registerData, "Register successfully");
    }

    @Operation(summary = "Logout", description = "Logout current device from sign in session")
    @PostMapping("/logout")
    public ApiResponse<?> logout(
            @RequestHeader(value = "X-User-Id") String userId,
            @RequestBody @Valid LogoutRequest logoutRequest) {
        authService.logout(userId, logoutRequest);
        return ApiResponse.builder()
                .code(200)
                .message("You have logged out!")
                .build();
    }
}
