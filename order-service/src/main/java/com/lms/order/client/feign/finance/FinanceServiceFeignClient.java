package com.lms.order.client.finance;

import com.lms.order.client.FeignClientConfig;
import com.lms.order.client.finance.dto.CreatePaymentDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "finance-service",
        configuration = FeignClientConfig.class
)
public interface FinanceServiceFeignClient {
    @PostMapping("/api/internal/v1/payments/create")
    String getPaymentUrl(@RequestBody CreatePaymentDto createReq);
}
