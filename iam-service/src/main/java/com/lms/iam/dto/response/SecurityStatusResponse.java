package com.lms.iam.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SecurityStatusResponse {
    boolean hasPin;
    boolean financialSessionActive;
    long remainingSeconds;
}
