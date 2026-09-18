package com.lms.finance.service;

import com.lms.finance.dto.request.CreatePaymentRequest;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface PaymentService {
    String createPayment (HttpServletRequest httpReq, CreatePaymentRequest createReq);
    void createProcessingPayment (CreatePaymentRequest createReq);
    Map<String, String> handlePaymentCallback (Map<String, String> params);
}
