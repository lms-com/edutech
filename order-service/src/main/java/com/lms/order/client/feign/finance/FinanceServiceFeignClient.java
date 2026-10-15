package com.lms.order.client.feign.finance;

import com.lms.order.client.feign.FeignClientConfig;
import com.lms.order.client.feign.finance.dto.CreatePaymentDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.lms.common.dto.response.ApiResponse;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "finance-service",
        configuration = FeignClientConfig.class
)
public interface FinanceServiceFeignClient {
    @PostMapping("/api/internal/v1/payments/create")
    String getPaymentUrl(@RequestBody CreatePaymentDto createReq);

    @PostMapping("/api/internal/v1/payments/refund/{orderId}")
    ApiResponse<Void> refundOrder(@PathVariable("orderId") String orderId);
}
