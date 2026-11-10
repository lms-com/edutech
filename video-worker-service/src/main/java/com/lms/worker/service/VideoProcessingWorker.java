package com.lms.worker.service;

import com.lms.common.exception.AppException;
import com.lms.worker.config.RabbitMQConfig;
import com.lms.worker.dto.message.VideoCompletedMessage;
import com.lms.worker.dto.message.VideoProcessMessage;
import com.lms.worker.exception.VideoWorkerErrorCode;
import com.lms.worker.model.MediaFile;
import com.lms.worker.model.MediaStatus;
import com.lms.worker.repository.MediaFileRepository;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.UploadObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;

import static com.lms.worker.config.RabbitMQConfig.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoProcessingWorker {

    private final MediaFileRepository mediaFileRepository;
    private final MinioClient minioClient;
    private final RabbitTemplate rabbitTemplate;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @RabbitListener(queues = RabbitMQConfig.VIDEO_PROCESSING_QUEUE)
    public void processVideo (VideoProcessMessage message) {

        // Lay MediaFile tu mediaId trong message
        String mediaId = message.getMediaId();

        MediaFile mediaFile = mediaFileRepository.findById(mediaId)
                .orElseThrow(() -> new AppException(VideoWorkerErrorCode.FILE_NOT_FOUND,
                        "Could not find media file with id " + mediaId));

        // Tao thu muc tam luu file input va thu muc luu cac file da xu li
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("worker_processing_" + mediaId);
            // Duong dan den file input tai ve tu Minio
            Path inputFilePath = tempDir.resolve(message.getStoredFileName());
            // Tao duong dan cho thu muc moi chua cac file da xu li
            Path outputDirPath = tempDir.resolve("hls_" + mediaId);
            // Tien hanh tao thu muc moi
            Files.createDirectories(outputDirPath);


            log.info("⏳ [WORKER] Downloading original file {} (mediaId: {})...", message.getStoredFileName(), mediaId);
            minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(message.getStoredFileName())
                            .build()
            ).transferTo(Files.newOutputStream(inputFilePath));

            // Chuan bi Key de ma hoa cac file .ts
            log.info("⏳ [WORKER] Đang sinh khóa AES-128 ngẫu nhiên...");
            byte[] aes = new byte[16];
            new SecureRandom().nextBytes(aes);
            // Chuyen aes thanh String de luu trong db
            String base64Key = Base64.getEncoder().encodeToString(aes);

            // Tao file enc.key de FFmpeg doc
            Path keyFilePath = tempDir.resolve("enc.key");
            Files.write( keyFilePath, aes);

            // Tao file enc.infokey chua duong dan cua file enc.key
            Path keyInfoFilePath = tempDir.resolve("enc.infokey");
            String infoContent =
                    "dummy_url_tam_thoi\n"
                            + keyFilePath.toAbsolutePath().toString();
            Files.writeString( keyInfoFilePath, infoContent);

            Path outputM3u8Path = outputDirPath.resolve("index.m3u8");

            // Bước 1: Ưu tiên Stream Copy trực tiếp (-c:v copy -c:a copy)
            // Giữ nguyên 100% chất lượng gốc, cực kỳ nhanh (2-5s cho file hàng trăm MB) và tiêu tốn cực ít RAM (<30MB)
            log.info("⏳ [WORKER] Đang bẻ chunk HLS và mã hóa AES-128 bằng Stream Copy (-c:v copy -c:a copy)...");
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "ffmpeg",
                    "-y",
                    "-i", inputFilePath.toString(),
                    "-c:v", "copy",
                    "-c:a", "copy",
                    "-start_number", "0",
                    "-hls_time", "10",
                    "-hls_list_size", "0",
                    "-hls_key_info_file", keyInfoFilePath.toString(),
                    "-f", "hls",
                    outputM3u8Path.toString()
            );
            processBuilder.redirectErrorStream(true);
            processBuilder.inheritIO();
            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            // Bước 2: Nếu video gốc dùng codec không tương thích với MPEG-TS hoặc bitstream lỗi,
            // Fallback sang encode lại có giới hạn tài nguyên nghiêm ngặt (-threads 2) để không vượt quá giới hạn RAM
            if (exitCode != 0) {
                log.warn("⚠️ Stream copy không thành công (exitCode: {}). Tiến hành encode lại có kiểm soát bộ nhớ (-threads 2)...", exitCode);
                ProcessBuilder fallbackBuilder = new ProcessBuilder(
                        "ffmpeg",
                        "-y",
                        "-threads", "2",
                        "-i", inputFilePath.toString(),
                        "-c:v", "libx264",
                        "-preset", "veryfast",
                        "-crf", "23",
                        "-threads", "2",
                        "-c:a", "aac",
                        "-b:a", "128k",
                        "-start_number", "0",
                        "-hls_time", "10",
                        "-hls_list_size", "0",
                        "-hls_key_info_file", keyInfoFilePath.toString(),
                        "-f", "hls",
                        outputM3u8Path.toString()
                );
                fallbackBuilder.redirectErrorStream(true);
                fallbackBuilder.inheritIO();
                Process fallbackProcess = fallbackBuilder.start();
                exitCode = fallbackProcess.waitFor();
            }

            if (exitCode != 0) {
                throw new AppException(VideoWorkerErrorCode.FAILED_PROCESSING_FILE,
                        "Could not process file. Exit code: " + exitCode);
            }

            // upload may cai HLS nguoc len Minio lai
            log.info("⏳ [WORKER] Uploading HLS files to MinIO...");
            String minioHlsBasePath = "hls/" + mediaId + "/";
            File[] files = outputDirPath.toFile().listFiles();
            if (files != null) {
                for (File file : files) {
                    minioClient.uploadObject(
                            UploadObjectArgs.builder()
                                    .bucket(bucketName)
                                    .object(minioHlsBasePath + file.getName())
                                    .filename(file.getAbsolutePath())
                                    .build()
                    );
                }
            }

            // Cap nhat DB
            mediaFile.setStatus(MediaStatus.COMPLETED);
            mediaFile.setEncryptionKey(base64Key);
            mediaFile.setHlsManifestUrl(minioHlsBasePath + "index.m3u8");
            mediaFileRepository.save(mediaFile);

            log.info("✅ [WORKER] Completed! HLS Link: {}", mediaFile.getHlsManifestUrl());

            // Gui message thong bao Video was processed completedly len RabbitMQ
            VideoCompletedMessage videoCompletedMessage =
                    new VideoCompletedMessage(mediaId);
            rabbitTemplate.convertAndSend(
                    MEDIA_EXCHANGE,
                    VIDEO_COMPLETED_ROUTING_KEY,
                    videoCompletedMessage
            );
            log.info("✅ [WORKER] VIDEO COMPLETED MESSAGE WAS SENT: {}", mediaId);

            // Xoa file video mp4 goc trong minio
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(message.getStoredFileName())
                            .build()
            );
            log.info("✅ [WORKER] ORIGINAL VIDEO {} WAS DELETED: ", mediaFile.getStoredFileName());

        } catch (Exception e) {
            log.error("❌ [WORKER] Failed: {}", e.getMessage());
            mediaFile.setStatus(MediaStatus.FAILED);
            mediaFileRepository.save(mediaFile);
        } finally {
            // kiem tra de tranh loi NullPointerException
            if (tempDir != null) {
                deleteDirectory(tempDir.toFile());
            }
        }
    }



    private void deleteDirectory (File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        directoryToBeDeleted.delete();
    }
}
