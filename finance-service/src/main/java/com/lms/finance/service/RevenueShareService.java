package com.lms.finance.service;

import com.lms.finance.dto.message.OrderCompletedMessage;

public interface RevenueShareService {
    void processRevenueDistribution (OrderCompletedMessage message);
    void processRefund(String orderId);
}
