package com.lms.course.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VideoStreamHeartbeatResponse {

    boolean active;
    boolean conflict;
    String message;
    String currentSessionId;
    String activeDevice;
}
