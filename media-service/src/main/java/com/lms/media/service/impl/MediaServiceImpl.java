package com.lms.media.service.impl;

import com.lms.common.exception.AppException;
import com.lms.media.client.course.CourseServiceFeignClient;
import com.lms.media.dto.message.VideoProcessMessage;
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
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.lms.media.config.RabbitMQConfig.MEDIA_EXCHANGE;
import static com.lms.media.config.RabbitMQConfig.VIDEO_PROCESSING_ROUTING_KEY;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaServiceImpl implements MediaService {

    private final MinioService minioService;
    private final MediaFileRepository mediaFileRepository;
    private final RabbitTemplate rabbitTemplate;
    private final CourseServiceFeignClient courseServiceFeignClient;
    private final StringRedisTemplate redisTemplate;

    @Value("${minio.bucket-name}")
    private String bucketName;

    /**
     * Cac Service cho Instructor
     */

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

        // Chuyen status thanh PROCESSING khi file ton tai
        mediaFile.setStatus(MediaStatus.PROCESSING);
        mediaFileRepository.save(mediaFile);
        // Gui tin nhan vao RabbitMQ
        VideoProcessMessage videoProcessMessage = new VideoProcessMessage(mediaId, mediaFile.getStoredFileName());
        rabbitTemplate.convertAndSend(
                MEDIA_EXCHANGE,
                VIDEO_PROCESSING_ROUTING_KEY,
                videoProcessMessage
        );
        log.info("media file {} has been confirmed", mediaId);
    }


    @Scheduled(fixedRate = 60000)
    @Override
    @Transactional
    public void autoCleanPendingFilesAfter12Hours () {
        Instant threshold = Instant.now().minusSeconds(70);
        mediaFileRepository.deleteByStatusAndUpdatedAtBefore(MediaStatus.PENDING, threshold);
        log.info("🧹 Auto clean pending files after 12 hours");
    }


    @Override
    @Transactional
    public void removeFile(String mediaId) {
        MediaFile file = mediaFileRepository.findById(mediaId)
                        .orElse(null);
        if (file == null) {
            log.warn("media file {} not found.", mediaId);
            return;
        }
        minioService.removeFile(file.getStoredFileName());
        mediaFileRepository.deleteById(mediaId);
        log.info("Delete file {} from Database successfully", mediaId);
    }


    /**
     * Cac Service cho Learner
     */

    @Override
    @Transactional
    public String getVideoManifest (String learnerId, String mediaId) {
        MediaFile mediaFile = mediaFileRepository.findById(mediaId)
                .orElseThrow(() -> new AppException(MediaErrorCode.FILE_NOT_FOUND, "media file " + mediaId + " not found."));
        if (mediaFile.getStatus() != MediaStatus.COMPLETED) {
            throw new AppException(MediaErrorCode.FILE_NOT_AVAILABLE, "media file " + mediaFile.getStoredFileName() + " has not been completed.");
        }

        // Goi kiem tra user nay da mua khoa hoc cua video nay chua (Feign client: course-service)
        boolean isAccessible = courseServiceFeignClient.isEnrolled(learnerId, mediaId);
        if (!isAccessible) {
            log.warn("❌ User {} has not been enrolled Course containing media file {}", learnerId, mediaFile.getOriginalFileName());
            throw new AppException(MediaErrorCode.UNACCEPTABLE, "user " + learnerId + "has not been enrolled Course containing media file " + mediaFile.getOriginalFileName());
        }

        // Neu hop le -> Tao SessionId
        String sessionId = UUID.randomUUID().toString();
        // Luu SessionId vao Redis kem deviceFingerPrint cua user voi TTL = 1 phut
        String redisKey = "media_session:" + sessionId;
        redisTemplate.opsForValue().set(redisKey, learnerId, 1, TimeUnit.MINUTES);

        // Lay file goc tu minio
        String manifestPath = mediaFile.getHlsManifestUrl();        // hls/{uuid}/index.m3u8
        String folderPath = manifestPath.substring(0, manifestPath.lastIndexOf('/') + 1);    // lay phan dia chi folder hls/{uuid}/
        try {
            // Lay va thay noi dung file manifest
            var indexFile = minioService.getFile(manifestPath);

            // Chinh sua noi dung file
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(indexFile))){
                return reader.lines().map(line -> {
                    // Thay url xin key trong file index.m3u8
                    if (line.startsWith("#EXT-X-KEY")) {
                        String keyUrl = "http://localhost:8080/media-service/api/v1/media/secure/key/" + mediaId + "?session=" + sessionId;
                        return line.replaceAll("URI=\".*?\"", "URI=\"" + keyUrl + "\"");
                    }

                    // Thay ten file .ts thanh uri den chinh no
                    if (line.endsWith(".ts")) {
                        String tsObjectName = folderPath + line;  // hls/{uuid}/{tenfile.ts}
                        return minioService.generatePresignedGetUrl(tsObjectName, 10, TimeUnit.MINUTES);
                    }

                    // Cac dong khac
                    return line;
                }).collect(Collectors.joining("\n"));
            }
        }catch (Exception e) {
            log.error("Lỗi khi sinh Dynamic Manifest: ", e);
            throw new RuntimeException("Không thể tạo luồng phát Video");
        }
    }


    @Transactional(readOnly = true)
    @Override
    public byte[] getEncryptionKey(String learnerId, String mediaId, String sessionId) {
        // Kiem tra sessionId
        String redisKey = "media_session:" + sessionId;
        String userRequestingKey = redisTemplate.opsForValue().get(redisKey);
        // Neu session het han (= null) hoac nguoi khac mao danh (!= learnerId)
        if (userRequestingKey == null || !userRequestingKey.equals(learnerId)) {
            log.warn("🚨 Attack alert: Unauthorized key dectected or Session has expired! User: {}, Media: {}", learnerId, mediaId);
            throw new AppException(MediaErrorCode.UNAUTHORIZED, "Invalid or expired Session!");
        }
        // Hop le, lay mediaFile ra
        MediaFile mediaFile = mediaFileRepository.findById(mediaId)
                .orElseThrow(() -> new AppException(MediaErrorCode.FILE_NOT_FOUND, "media file " + mediaId + " not found."));
        String base64Key = mediaFile.getEncryptionKey();
        if (base64Key == null || base64Key.isEmpty()) {
            throw new AppException(MediaErrorCode.LACK_OF_ENCRYPTION_KEY, "Media file " + mediaFile.getStoredFileName() + " has not been encrypted.");
        }

        // Xoa luon Session trong Redis vi het gia tri su dung
        redisTemplate.delete(redisKey);

        return Base64.getDecoder().decode(base64Key);
    }


    /**
     * Cac Service chi danh cho Admin
     */

    @Override
    public List<MediaFile> getAllMediaFiles() {
        return mediaFileRepository.findAll();
    }
}
