package com.lms.order.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

/**
 * Kết quả validate mã giảm giá trước khi đặt hàng.
 * Trả về đủ thông tin để Frontend hiển thị preview giá tiết kiệm.
 */
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PromotionValidateResponse {

    boolean valid;
    String code;
    String message;

    // Nếu valid = true, trả về thông tin giảm giá
    BigDecimal discountPercent;
    BigDecimal discountAmount;   // Số tiền thực tế được giảm (tính từ giá khóa học)
    BigDecimal originalPrice;    // Giá gốc khóa học
    BigDecimal finalPrice;       // Giá sau khi áp mã
}
