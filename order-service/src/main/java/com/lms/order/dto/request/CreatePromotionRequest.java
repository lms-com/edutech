package com.lms.order.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreatePromotionRequest {

    @NotBlank(message = "Mã giảm giá không được để trống")
    @Size(min = 3, max = 20, message = "Mã giảm giá phải từ 3 đến 20 ký tự")
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "Mã giảm giá chỉ chứa chữ hoa, số, gạch ngang và gạch dưới")
    String code;

    // Giảm theo % (0..100)
    @DecimalMin(value = "0.01", message = "Phần trăm giảm phải lớn hơn 0")
    @DecimalMax(value = "100.00", message = "Phần trăm giảm không được vượt quá 100%")
    BigDecimal discountPercent;

    // Giảm theo số tiền cố định (VND)
    @DecimalMin(value = "1000", message = "Số tiền giảm tối thiểu là 1,000đ")
    BigDecimal discountAmount;

    // Ngày bắt đầu (null = hiệu lực ngay)
    LocalDateTime startDate;

    // Ngày kết thúc (null = không giới hạn thời gian)
    LocalDateTime endDate;

    // Số lượt dùng tối đa (null = không giới hạn)
    Integer usageLimit;
}
