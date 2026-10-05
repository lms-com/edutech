package com.lms.iam.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminDeviceResponse {
    String deviceId; // fingerprint hoặc id
    String deviceFingerprint;
    String userId;
    String userEmail;
    String userFullName;
    @JsonFormat(pattern = "HH:mm:ss, dd-MM-yyyy")
    LocalDateTime loginAt;
    String lastActive;
    boolean isBlocked;
}
