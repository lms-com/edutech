package com.lms.finance.service.impl;

import com.lms.finance.dto.request.PayoutFilterRequest;
import com.lms.finance.dto.request.ProcessPayoutRequestDto;
import com.lms.finance.dto.response.PayoutRequestResponse;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;

public interface PayoutService {
    PayoutRequestResponse createPayoutRequest (String instructorId, BigDecimal amount);

    void approvePayoutRequest (String adminId, ProcessPayoutRequestDto processRequest);

    void rejectPayoutRequest (String adminId, ProcessPayoutRequestDto processRequest);

    Page<PayoutRequestResponse> getMyPayoutRequests (String instructorId, PayoutFilterRequest filter);

    Page<PayoutRequestResponse> getPayoutRequestsForAdmin(PayoutFilterRequest filter);
}
