package com.lms.course.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VideoStreamHeartbeatRequest {

    @NotBlank(message = "SessionId không được để trống")
    String sessionId;

    String deviceFingerprint;

    String clientPlatform;
}
