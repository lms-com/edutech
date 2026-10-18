package com.lms.iam.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ResetPinRequest {

    @NotBlank(message = "Mã OTP không được để trống")
    @Pattern(regexp = "^\\d{6}$", message = "Mã OTP phải gồm đúng 6 chữ số")
    String otp;

    @NotBlank(message = "Mã PIN mới không được để trống")
    @Pattern(regexp = "^\\d{6}$", message = "Mã PIN mới phải gồm đúng 6 chữ số")
    String newPin;
}
