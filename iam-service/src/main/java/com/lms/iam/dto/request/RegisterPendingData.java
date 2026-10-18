package com.lms.iam.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RegisterPendingData {
    String email;
    String fullName;
    String passwordHash;
    String otpCode;
}
