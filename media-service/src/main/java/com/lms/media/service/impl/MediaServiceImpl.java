package com.lms.media.service.impl;

import com.lms.common.exception.AppException;
import com.lms.media.dto.request.GetUploadUrlRequest;
import com.lms.media.dto.response.GetUploadUrlResponse;
import com.lms.media.exception.MediaErrorCode;
import com.lms.media.model.MediaFile;
import com.lms.media.model.MediaStatus;
import com.lms.media.repository.MediaFileRepository;
import com.lms.media.service.MediaService;

import com.lms.media.service.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaServiceImpl implements MediaService {

    private final MinioService minioService;
    private final MediaFileRepository mediaFileRepository;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Override
    @Transactional
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


    @Override
    @Transactional
    public void confirmUploadUrl(String mediaId) {
        // Kiem tra co mediaId trong Media File repository khong
        MediaFile mediaFile = mediaFileRepository.findById(mediaId)
                .orElseThrow(() -> new AppException(MediaErrorCode.FILE_NOT_FOUND, "media file " + mediaId + " not found."));

        // Kiem tra trong Minio co file hay khong

        boolean exists = minioService.isObjectExist(mediaFile.getStoredFileName());
        if (!exists) {
            log.warn("media file {} does not exist", mediaId);
            throw new AppException(MediaErrorCode.FILE_NOT_FOUND, "media file " + mediaId + " not found.");
        }

        // Chuyen status thanh COMPLETED khi file ton tai
        mediaFile.setStatus(MediaStatus.COMPLETED);
        mediaFileRepository.save(mediaFile);
        log.info("media file {} has been confirmed", mediaId);
    }


    @Override
    @Transactional
    public String getDisplayUrl(String mediaId) {
        MediaFile mediaFile = mediaFileRepository.findById(mediaId)
                .orElseThrow(() -> new AppException(MediaErrorCode.FILE_NOT_FOUND, "media file " + mediaId + " not found."));
        if (mediaFile.getStatus() != MediaStatus.COMPLETED) {
            throw new AppException(MediaErrorCode.FILE_NOT_AVAILABLE, "media file " + mediaFile.getStoredFileName() + " has not been completed.");
        }
        return minioService.generatePresignedGetUrl(
                mediaFile.getStoredFileName(), 2, TimeUnit.HOURS
        );
    }
}
