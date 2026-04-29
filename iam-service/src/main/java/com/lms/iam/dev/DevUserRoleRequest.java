package com.lms.iam.dev;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DevUserRoleRequest {
    @NotBlank
    String userId;
    @NotBlank
    String roleName;
}
