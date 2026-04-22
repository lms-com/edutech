package com.lms.iam.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LoginRequest {
    @NotBlank( message = "Email is required")
    @Email( message = "Invalid email format")
    String email;

    @NotBlank( message = "Password is required")
//    @Size(min = 8, message = "Password must be at least 8 characters")
//    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).*$",
//            message = "Password must contain at least 1 uppercase letter, 1 lowercase letter, 1 number and 1 special character")
    String password;

    @NotBlank( message = "Device fingerprint is required")
    String deviceFingerPrint;
}
