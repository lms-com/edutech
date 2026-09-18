package com.lms.order.client.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignClientInterceptor implements RequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(FeignClientInterceptor.class);
    @Value("${application.security.internal-key}")
    private String internalKey;

    @Override
    public void apply(RequestTemplate requestTemplate) {
        log.info("🔑 Internal Key from Order: {}", internalKey);
        requestTemplate.header("X-Internal-key", internalKey);
    }
}
