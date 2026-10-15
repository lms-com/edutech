package com.lms.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateRefundRequest {
    @NotBlank(message = "Lý do khiếu nại không được để trống")
    String reason;

    String detail;
}
