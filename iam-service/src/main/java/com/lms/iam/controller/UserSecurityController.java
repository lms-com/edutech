package com.lms.iam.controller;

import com.lms.common.dto.response.ApiResponse;
import com.lms.common.swagger.annotation.RequireJwt;
import com.lms.iam.dto.request.*;
import com.lms.iam.dto.response.SecurityStatusResponse;
import com.lms.iam.service.UserSecurityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "User Security Controller", description = "Quản lý bảo mật tài chính cấp 2, Mã PIN ví và Đổi mật khẩu qua OTP")
@RequestMapping("/api/v1/user/security")
@RequireJwt
@RequiredArgsConstructor
public class UserSecurityController {

    private final UserSecurityService securityService;

    @Operation(summary = "Lấy trạng thái bảo mật ví", description = "Kiểm tra người dùng đã tạo PIN chưa và phiên tài chính còn hạn bao lâu")
    @GetMapping("/status")
    public ApiResponse<SecurityStatusResponse> getSecurityStatus(@RequestHeader("X-User-Id") String userId) {
        return ApiResponse.success(securityService.getSecurityStatus(userId));
    }

    @Operation(summary = "Thiết lập mã PIN ví lần đầu", description = "Cài đặt mã PIN 6 số cho ví và tự động kích hoạt phiên 15 phút")
    @PostMapping("/pin/setup")
    public ApiResponse<String> setupPin(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody @Valid SetupPinRequest request) {
        securityService.setupPin(userId, request);
        return ApiResponse.success("Thiết lập mã PIN ví thành công. Phiên tài chính đã được mở khóa 15 phút.");
    }

    @Operation(summary = "Đổi mã PIN ví", description = "Đổi mã PIN 6 số bằng mã PIN hiện tại")
    @PostMapping("/pin/change")
    public ApiResponse<String> changePin(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody @Valid ChangePinRequest request) {
        securityService.changePin(userId, request);
        return ApiResponse.success("Đổi mã PIN ví thành công.");
    }

    @Operation(summary = "Mở khóa phiên tài chính bằng mã PIN", description = "Xác thực mã PIN để truy cập dashboard quản lý tiền trong 15 phút")
    @PostMapping("/pin/verify")
    public ApiResponse<String> verifyPin(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody @Valid VerifyPinRequest request) {
        securityService.verifyPin(userId, request);
        return ApiResponse.success("Mở khóa phiên bảo mật tài chính thành công (15 phút).");
    }

    @Operation(summary = "Yêu cầu OTP khôi phục mã PIN", description = "Gửi mã OTP 6 số về email đăng ký để đặt lại mã PIN")
    @PostMapping("/pin/forgot-otp")
    public ApiResponse<String> sendResetPinOtp(@RequestHeader("X-User-Id") String userId) {
        securityService.sendResetPinOtp(userId);
        return ApiResponse.success("Mã OTP khôi phục mã PIN đã được gửi đến email của bạn.");
    }

    @Operation(summary = "Đặt lại mã PIN bằng OTP", description = "Đặt mã PIN mới sử dụng mã OTP nhận qua email")
    @PostMapping("/pin/reset")
    public ApiResponse<String> resetPin(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody @Valid ResetPinRequest request) {
        securityService.resetPin(userId, request);
        return ApiResponse.success("Đặt lại mã PIN thành công. Phiên tài chính đã được mở khóa 15 phút.");
    }

    @Operation(summary = "Khóa phiên tài chính ngay", description = "Chủ động khóa quyền truy cập bảng quản lý tiền")
    @PostMapping("/session/lock")
    public ApiResponse<String> lockFinancialSession(@RequestHeader("X-User-Id") String userId) {
        securityService.lockFinancialSession(userId);
        return ApiResponse.success("Đã khóa phiên tài chính an toàn.");
    }

    @Operation(summary = "Gửi OTP đổi mật khẩu tài khoản", description = "Gửi mã xác thực về email trước khi cho phép đổi mật khẩu")
    @PostMapping("/password/send-otp")
    public ApiResponse<String> sendChangePasswordOtp(@RequestHeader("X-User-Id") String userId) {
        securityService.sendChangePasswordOtp(userId);
        return ApiResponse.success("Mã OTP xác thực đổi mật khẩu đã được gửi đến email của bạn.");
    }

    @Operation(summary = "Đổi mật khẩu tài khoản bằng OTP", description = "Đổi mật khẩu mới kèm xác thực OTP từ email")
    @PostMapping("/password/change-with-otp")
    public ApiResponse<String> changePasswordWithOtp(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody @Valid ChangePasswordWithOtpRequest request) {
        securityService.changePasswordWithOtp(userId, request);
        return ApiResponse.success("Đổi mật khẩu tài khoản thành công! Các phiên đăng nhập trên thiết bị khác đã được thu hồi.");
    }
}
