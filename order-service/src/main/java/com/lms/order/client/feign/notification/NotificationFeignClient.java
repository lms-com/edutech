package com.lms.order.client.feign.notification;

import com.lms.common.dto.response.ApiResponse;
import com.lms.order.client.feign.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "notification-service", path = "/api/internal/v1/notifications", configuration = FeignClientConfig.class)
public interface NotificationFeignClient {

    @PostMapping("/send-custom")
    ApiResponse<Void> sendCustomNotification(
            @RequestParam("userId") String userId,
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam("type") String type,
            @RequestParam(value = "refId", required = false) String refId,
            @RequestParam(value = "refType", required = false) String refType);
}
