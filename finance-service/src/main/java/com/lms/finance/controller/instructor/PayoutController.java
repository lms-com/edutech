package com.lms.finance.controller.instructor;

import com.lms.common.dto.response.ApiResponse;
import com.lms.common.swagger.annotation.RequireJwt;
import com.lms.finance.dto.request.PayoutFilterRequest;
import com.lms.finance.dto.response.PayoutRequestResponse;
import com.lms.finance.service.impl.PayoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Tag(name = "Payout Apis for Instructor")
@RestController
@RequestMapping("/api/v1/instructor/payouts")
@RequiredArgsConstructor
@RequireJwt
public class PayoutController {
    private final PayoutService payoutService;

    @Operation(summary = "Create a new Payout request")
    @PostMapping
    public ApiResponse<PayoutRequestResponse> createPayoutRequest (@RequestHeader("X-User-Id") String instructorId, @RequestBody BigDecimal amount) {
        return ApiResponse.success(payoutService.createPayoutRequest(instructorId, amount));
    }

    @Operation(summary = "Get own Payout Requestes")
    @GetMapping
    public ApiResponse<Page<PayoutRequestResponse>> getOwnPayoutRequestes (
            @RequestHeader("X-User-Id") String instructorId,
            @Valid @ParameterObject PayoutFilterRequest filter
    ) {
        return ApiResponse.success(payoutService.getMyPayoutRequests(instructorId, filter));
    }
}
