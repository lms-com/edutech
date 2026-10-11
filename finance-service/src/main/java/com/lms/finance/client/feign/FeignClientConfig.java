package com.lms.finance.client.feign;

import feign.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignClientConfig {

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.HEADERS;
    }

    @Bean
    public feign.RequestInterceptor feignRequestInterceptor(@org.springframework.beans.factory.annotation.Value("${application.security.internal-key}") String internalKey) {
        return template -> template.header("X-Internal-Key", internalKey);
    }
}
