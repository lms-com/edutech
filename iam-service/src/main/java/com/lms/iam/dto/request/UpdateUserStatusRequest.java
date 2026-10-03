package com.lms.iam.dto.request;

import com.lms.iam.model.Userstatus;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserStatusRequest {
    private Userstatus status;
    private String reason;
}
