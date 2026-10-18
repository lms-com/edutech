package com.lms.finance.controller.instructor;

import com.lms.common.dto.response.ApiResponse;
import com.lms.common.swagger.annotation.RequireJwt;
import com.lms.finance.dto.request.UpdateBankAccountRequest;
import com.lms.finance.dto.response.BankAccountResponse;
import com.lms.finance.service.BankAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Bank Account Apis for Instructor")
@RestController
@RequestMapping("/api/v1/instructor/bank-accounts")
@RequiredArgsConstructor
@RequireJwt
public class BankAccountController {

    private final BankAccountService bankAccountService;

    @Operation(summary = "Lấy thông tin tài khoản ngân hàng chính của giảng viên")
    @GetMapping("/primary")
    public ApiResponse<BankAccountResponse> getPrimaryBankAccount(@RequestHeader("X-User-Id") String instructorId) {
        return ApiResponse.success(bankAccountService.getPrimaryBankAccount(instructorId));
    }

    @Operation(summary = "Liên kết hoặc cập nhật tài khoản ngân hàng chính")
    @PostMapping
    public ApiResponse<BankAccountResponse> saveOrUpdateBankAccount(
            @RequestHeader("X-User-Id") String instructorId,
            @Valid @RequestBody UpdateBankAccountRequest request
    ) {
        return ApiResponse.success(bankAccountService.saveOrUpdateBankAccount(instructorId, request));
    }
}
