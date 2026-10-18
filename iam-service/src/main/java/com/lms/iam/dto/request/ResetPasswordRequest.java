package com.lms.iam.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Yêu cầu đặt lại mật khẩu bằng mã OTP")
public class ResetPasswordRequest {

    @Schema(description = "Email người dùng", example = "learner@edutech.vn")
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Định dạng email không hợp lệ")
    String email;

    @Schema(description = "Mã OTP 6 chữ số", example = "123456")
    @NotBlank(message = "Mã OTP không được để trống")
    @Pattern(regexp = "^\\d{6}$", message = "Mã OTP phải gồm 6 chữ số")
    String otp;

    @Schema(description = "Mật khẩu mới", example = "NewPass123@")
    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(min = 6, max = 50, message = "Mật khẩu mới phải có từ 6 đến 50 ký tự")
    String newPassword;
}
