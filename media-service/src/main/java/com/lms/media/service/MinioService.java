package com.lms.media.service;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    public String generatePresignedUploadUrl (String fileName) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(bucketName)
                            .object(fileName)
                            .expiry(15, TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isObjectExist (String fileName) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build()

            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String generatePresignedGetUrl (String fileName, int expire, TimeUnit timeUnit) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(fileName)
                            .expiry(expire, timeUnit)
                            .build()
            );
        } catch (Exception e) {
            log.error("❌ Error while get file {} from Minio: {}", fileName, e.getMessage());
            throw new RuntimeException("❌ Failed to get file from Minio" ,e);
        }
    }

    public void removeFile (String fileName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build()
            );
            log.info("Removed file {} from Minio", fileName);
        } catch (Exception e) {
            log.error("Failed to remove file {} from Minio", fileName, e);
            throw new RuntimeException("❌ Cannot remove file from Bucket", e);
        }
    }
}
