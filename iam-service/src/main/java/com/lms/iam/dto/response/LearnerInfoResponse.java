package com.lms.iam.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LearnerInfoResponse {
    String id;
    String fullName;
    String email;
    String avatarUrl;
}
