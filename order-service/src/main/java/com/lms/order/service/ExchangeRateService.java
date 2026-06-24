package com.lms.order.service;

import com.lms.common.exception.AppException;
import com.lms.order.exception.OrderErrorCode;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class ExchangeRateService {

    public BigDecimal getRate (String fromCurrency, String toCurrency) {
        if (fromCurrency.equalsIgnoreCase(toCurrency)) {
            return BigDecimal.ONE;
        }

        if (fromCurrency.equalsIgnoreCase("USD") && toCurrency.equalsIgnoreCase("VND"))
            return BigDecimal.valueOf(25450.00);

        if (fromCurrency.equalsIgnoreCase("VND") && toCurrency.equalsIgnoreCase("USD"))
            return BigDecimal.ONE.divide(BigDecimal.valueOf(25450.00), 6, RoundingMode.HALF_UP);

        throw new AppException(OrderErrorCode.CURRENCY_NOT_SUPPORTED);
    }
}