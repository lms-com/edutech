package com.lms.finance.controller.instructor;

import com.lms.common.dto.response.ApiResponse;
import com.lms.finance.dto.request.BalanceHistoryFilterRequest;
import com.lms.finance.dto.response.BalanceHistoryInstructorResponse;
import com.lms.finance.dto.response.BalanceInstructorResponse;
import com.lms.finance.service.BalanceHistoryService;
import com.lms.finance.service.InstructorBalanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Apis for balances")
@RequestMapping("/api/v1/instructor/balances")
@RestController
@RequiredArgsConstructor
public class BalanceController {
    private final InstructorBalanceService balanceService;
    private final BalanceHistoryService historyService;

    @Operation(summary = "Get balances")
    @GetMapping
    public ApiResponse<BalanceInstructorResponse> getMyBalances(@RequestHeader(value = "X-User-Id") String userId) {
        return ApiResponse.success(balanceService.getMyBalances(userId));
    }


    @Operation(summary = "Get own balance histories")
    @GetMapping("/histories")
    public ApiResponse<Page<BalanceHistoryInstructorResponse>> getMyBalanceHistories (
            @RequestHeader("X-User-Id") String userId,
            @Valid @ParameterObject BalanceHistoryFilterRequest filter
    ) {
        return ApiResponse.success(historyService.getMyBalanceHistories(userId, filter));
    }

}
