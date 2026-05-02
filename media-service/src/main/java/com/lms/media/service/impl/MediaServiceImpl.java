package com.lms.media.service.impl;

import com.lms.media.dto.request.GetUploadUrlRequest;
import com.lms.media.dto.response.GetUploadUrlResponse;
import com.lms.media.model.MediaFile;
import com.lms.media.model.MediaStatus;
import com.lms.media.repository.MediaFileRepository;
import com.lms.media.service.MediaService;

import com.lms.media.service.MinioService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaServiceImpl implements MediaService {

    private final MinioService minioService;
    private final MediaFileRepository mediaFileRepository;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Override
    public GetUploadUrlResponse requestUploadUrl(GetUploadUrlRequest request) {
        // Tao id cho file
        String fileId = UUID.randomUUID().toString();

        // Lay Extension cua file
        String extension = "";
        int dotIndex = request.getOriginalFileName().lastIndexOf('.');
        if (dotIndex > 0) {
            extension = request.getOriginalFileName().substring(dotIndex);
        }

        // Tao ten moi luu tren Minio cho file = fileId + extension
        String storedFileName = fileId + extension;

        // Luu thong tin MediaFile
        MediaFile mediaFile = MediaFile.builder()
                .id(fileId)
                .originalFileName(request.getOriginalFileName())
                .storedFileName(storedFileName)
                .bucketName(bucketName)
                .contentType(request.getContentType())
                .fileSize(request.getFileSize())
                .status(MediaStatus.PENDING)
                .build();
        mediaFileRepository.save(mediaFile);

        // Tao presigned upload url
        String presignedUrl = minioService.generatePresignedUploadUrl(storedFileName);

        // Tra ve Response
        return GetUploadUrlResponse.builder()
                .mediaId(fileId)
                .uploadUrl(presignedUrl)
                .build();
    }
}
