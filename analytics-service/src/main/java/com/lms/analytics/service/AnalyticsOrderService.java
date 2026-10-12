package com.lms.analytics.service;

import com.lms.analytics.dto.message.OrderCompletedMessage;

public interface AnalyticsOrderService {
    void processOrderCompleted(OrderCompletedMessage message);
}
