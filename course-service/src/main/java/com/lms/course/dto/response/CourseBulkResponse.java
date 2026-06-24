package com.lms.course.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * API 40: Response nội bộ cho Finance Service.
 * Trả về thông tin cơ bản + giá + instructorId để phục vụ Revenue Split.
 */
@Data
@Builder
public class CourseBulkResponse {
    private String id;
    private String title;
    private BigDecimal basePrice;
    private String currencyCode;
    private String instructorId;
}
