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
public class SetupPinRequest {

    @NotBlank(message = "Mã PIN không được để trống")
    @Pattern(regexp = "^\\d{6}$", message = "Mã PIN bảo mật phải gồm đúng 6 chữ số")
    String pin;
}
