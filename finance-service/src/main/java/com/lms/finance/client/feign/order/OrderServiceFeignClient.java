package com.lms.finance.client.order;

import com.lms.finance.client.FeignClientConfig;
import com.lms.finance.client.order.dto.CourseInternalRequest;
import com.lms.finance.client.order.dto.OrderDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(
            name = "order-service",
            configuration = FeignClientConfig.class
    )
public interface OrderServiceFeignClient {

    @GetMapping("/api/internal/v1/orders/{id}")
    public OrderDto getOrderById (@PathVariable("id") String orderId);

    @GetMapping("/api/internal/v1/orders/{id}/courses")
    public List<CourseInternalRequest> getCoursesByOrderId (@PathVariable("id") String orderId);
}
