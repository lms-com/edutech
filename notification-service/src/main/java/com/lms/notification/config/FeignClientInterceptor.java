package com.lms.notification.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Gắn X-Internal-Key cho mọi lời gọi Feign ra ngoài.
 *
 * Cần thiết để gọi được các API /api/internal/v1/** của iam-service và
 * course-service, vốn bị InternalApiFilter chặn nếu thiếu header.
 */
@Configuration
public class FeignClientInterceptor implements RequestInterceptor {

    @Value("${application.security.internal-key}")
    private String internalKey;

    @Override
    public void apply(RequestTemplate requestTemplate) {
        requestTemplate.header("X-Internal-Key", internalKey);
    }
}
