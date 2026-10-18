package com.lms.iam.dev;

import lombok.*;
import org.springframework.context.annotation.Profile;

@Profile("dev")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DevUpdateUserPasswordRequest {
    private String userId;
    private String newPassword;
}
