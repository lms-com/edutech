package com.lms.media.client;

import feign.Logger;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignClientConfig {

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.HEADERS;
    }

    @Bean
    public RequestInterceptor feignRequestInterceptor(@Value("${application.security.internal-key}") String internalKey) {
        return template -> template.header("X-Internal-Key", internalKey);
    }
}
