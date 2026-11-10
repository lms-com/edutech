package com.lms.media.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class MediaConfig {

    @Value("${minio.uri}")
    private String uri;

    @Value("${minio.external-uri:${minio.uri}}")
    private String externalUri;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Bean
    @Primary
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(uri)
                .region("us-east-1")
                .credentials(accessKey, secretKey)
                .build();
    }

    @Bean(name = "minioClientExternal")
    public MinioClient minioClientExternal() {
        return MinioClient.builder()
                .endpoint(externalUri)
                .region("us-east-1")
                .credentials(accessKey, secretKey)
                .build();
    }
}
