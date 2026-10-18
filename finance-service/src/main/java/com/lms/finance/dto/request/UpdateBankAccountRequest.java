package com.lms.finance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateBankAccountRequest {

    @NotBlank(message = "Mã ngân hàng không được để trống")
    @Size(max = 20, message = "Mã ngân hàng tối đa 20 ký tự")
    String bankCode;

    @NotBlank(message = "Số tài khoản ngân hàng không được để trống")
    @Size(min = 6, max = 50, message = "Số tài khoản phải từ 6 đến 50 ký tự")
    @Pattern(regexp = "^[0-9A-Za-z]+$", message = "Số tài khoản chỉ chứa số hoặc chữ cái")
    String accountNumber;

    @NotBlank(message = "Tên chủ tài khoản không được để trống")
    @Size(max = 200, message = "Tên chủ tài khoản tối đa 200 ký tự")
    String accountName;
}
