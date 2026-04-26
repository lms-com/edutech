package com.lms.iam.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserDeviceResponse {
    private String deviceFingerprint;
    @JsonFormat(pattern = "HH:mm:ss, dd-MM-yyyy")
    LocalDateTime loginAt;
}
