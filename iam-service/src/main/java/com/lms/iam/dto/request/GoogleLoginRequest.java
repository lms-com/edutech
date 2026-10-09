package com.lms.iam.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GoogleLoginRequest {

    @NotBlank(message = "Google ID Token hoặc credential không được để trống")
    String idToken;

    @NotBlank(message = "Dấu vân tay thiết bị không được để trống")
    String deviceFingerPrint;
}
