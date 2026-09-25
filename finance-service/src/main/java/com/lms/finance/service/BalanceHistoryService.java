package com.lms.finance.service;

import com.lms.finance.dto.request.BalanceHistoryFilterRequest;
import com.lms.finance.dto.response.BalanceHistoryInstructorResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface BalanceHistoryService {
    Page<BalanceHistoryInstructorResponse> getMyBalanceHistories(String userId, BalanceHistoryFilterRequest filter);
}
