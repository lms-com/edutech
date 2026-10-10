package com.lms.media.client;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignClientInterceptor implements RequestInterceptor {

    @Value("${application.security.internal-key}")
    private String internalKey;

    @Override
    public void apply(RequestTemplate requestTemplate) {
        requestTemplate.header("X-Internal-key", internalKey);
    }
}
