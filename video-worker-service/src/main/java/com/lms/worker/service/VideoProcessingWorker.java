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
import java.io.IOException;
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


            log.info("⏳ [WORKER] Downloading the original file...");
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

            // Goi ffmpeg xu li
            log.info("⏳ [WORKER] Start operating FFmpeg (This process may take a few minutes)...");
            Path outputM3u8Path = outputDirPath.resolve("index.m3u8");
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "ffmpeg",               // Ten phan mem xu li
                    "-i", inputFilePath.toString(),    // Duong dan tuyet doi cua file goc sau khi tai ve
                    "-profile:v", "baseline",          // Cau hinh bo quy chuan nen: baseline la nhe nhat va tuong thich duoc voi thiet bi cu
                    "-level", "3.0",                   // Cau hinh gioi han bang thong: 3.0 phu hop voi do nen baseline o tren
                    "-start_number", "0",              // So thu tu bat dau cua cac file
                    "-hls_time", "10",                 // Do dai thoi gian cua cac file video nho sau khi xu li
                    "-hls_list_size", "0",             // Gioi han so luong file thanh pham: 0 la khong gioi han
                    "-hls_key_info_file", keyInfoFilePath.toString(),   // file chua thong tin path den key aes
                    "-f", "hls",                       // Kieu dinh dang: dinh dang kieu hls
                    outputM3u8Path.toString()          // Duong dan cua file tong index.m3u8
            );
            processBuilder.redirectErrorStream(true);
            processBuilder.inheritIO();
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
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
