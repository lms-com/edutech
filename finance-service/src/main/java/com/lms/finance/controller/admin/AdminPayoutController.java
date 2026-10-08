package com.lms.finance.controller.admin;

import com.lms.common.dto.response.ApiResponse;
import com.lms.common.swagger.annotation.RequireJwt;
import com.lms.finance.dto.request.PayoutFilterRequest;
import com.lms.finance.dto.request.ProcessPayoutRequestDto;
import com.lms.finance.dto.response.PayoutRequestResponse;
import com.lms.finance.service.impl.PayoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;


@Tag(name = "Payout Apis for Admin")
@RestController
@RequestMapping("/api/v1/admin/payouts")
@RequireJwt
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class AdminPayoutController {
    final PayoutService payoutService;

    @Operation(summary = "Approve a payout request")
    @PostMapping("/approve")
    public ApiResponse<String> approvePayoutRequest (@RequestHeader("X-User-Id") String adminId, @Valid @RequestBody ProcessPayoutRequestDto processRequest) {
        payoutService.approvePayoutRequest(adminId, processRequest);
        return ApiResponse.success("Approved PayoutRequest with id: " + processRequest.getPayoutId());
    }

    @Operation(summary = "Reject a payout request")
    @PostMapping("/reject")
    public ApiResponse<String> rejectPayoutRequest (@RequestHeader("X-User-Id") String adminId, @Valid @RequestBody ProcessPayoutRequestDto processRequest) {
        payoutService.rejectPayoutRequest(adminId, processRequest);
        return ApiResponse.success("Approved PayoutRequest with id: " + processRequest.getPayoutId());
    }

    @Operation(summary = "Get all Payout Request With filter for Admin")
    @GetMapping
    public ApiResponse<Page<PayoutRequestResponse>> getPayoutRequestsForAdmin(
            @Valid @ParameterObject PayoutFilterRequest filter
    ) {
        return ApiResponse.success(payoutService.getPayoutRequestsForAdmin(filter));
    }
}
