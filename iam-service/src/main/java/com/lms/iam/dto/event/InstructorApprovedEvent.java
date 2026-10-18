package com.lms.iam.dto.event;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InstructorApprovedEvent {

    String userId;
    String userEmail;
    String fullName;
    BigDecimal commissionRate;
    Instant approvedAt;
}
