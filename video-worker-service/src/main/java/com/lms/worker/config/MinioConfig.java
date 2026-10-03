package com.lms.worker.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    @Value("${minio.secret-key}")
    private String secretKey;
    @Value("${minio.access-key}")
    private String accessKey;
    @Value("${minio.uri}")
    private String uri;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(uri)
                .credentials(accessKey, secretKey)
                .build();
    }
}
