package com.lms.notification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class SseConfig {

    @Bean(name = "sseExecutor")
    public ThreadPoolTaskExecutor sseExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);      // Số lượng thread chạy thường trực cho kết nối quả chuông
        executor.setMaxPoolSize(100);      // Giới hạn tối đa 100 thread khi lượng truy cập tăng vọt
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("sse-worker-");
        executor.initialize();
        return executor;
    }
}