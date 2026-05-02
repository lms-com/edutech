package com.lms.media.config;

import com.lms.common.exception.AppException;
import com.lms.media.exception.MediaErrorCode;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MinioBucketInitializer {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    /**
     * Annotation @PostConstruct giúp hàm này tự động chạy MỘT LẦN DUY NHẤT
     * ngay khi media-service khởi động lên.
     */
    @PostConstruct
    public void initBucket() {
        try {
            // Goi ham kiem tra bucket ton tai chua
            boolean bucketExists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build()
            );

            // Neu bucket chua ton tai thi tao bucket moi
            if (!bucketExists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucketName).build()
                );
                log.info("Make bucket Successfully");
            } else {
                log.info("Bucket already exists");
            }
        } catch (Exception e) {
            log.error("Error while initializing Minio Bucket Initializer", e);
            throw new AppException(MediaErrorCode.INITIALIZE_BUCKET_MINIO_FAILED);
        }
    }
}