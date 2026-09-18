package com.lms.finance.service;

import com.lms.finance.dto.request.CreatePaymentRequest;
import jakarta.servlet.http.HttpServletRequest;

public interface PaymentStrategy {
    String processPayment (HttpServletRequest req, CreatePaymentRequest reqInfo);
}
