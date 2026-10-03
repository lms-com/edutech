package com.lms.notification.config;

import io.minio.MinioClient;
import lombok.val;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.access-key}")
    private String accessKey;

    /**
     * Khoá cấu hình là `minio.uri` (application.yml) và CertificateServiceImpl cũng
     * đọc `minio.uri`. Trước đây chỗ này đọc `minio.url` nên không có giá trị và
     * notification-service không thể khởi động.
     */
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