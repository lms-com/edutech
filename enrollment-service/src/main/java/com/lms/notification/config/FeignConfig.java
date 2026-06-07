package com.lms.notification.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    @Value("${application.security.internal-key:my-secret-internal-key}")
    String internalKey;

    // Tự động đính kèm X-Internal-Key vào mọi FeignClient call
    @Bean
    public RequestInterceptor internalKeyInterceptor() {
        return requestTemplate ->
            requestTemplate.header("X-Internal-Key", internalKey);
    }
}
