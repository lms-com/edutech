package com.lms.iam.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReviewInstructorApplicationRequest {

    Boolean approved;

    String rejectionReason;

    BigDecimal defaultCommissionRate;
}
