package com.lms.notification.service.impl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lms.common.exception.AppException;
import com.lms.common.dto.response.ApiResponse;
import com.lms.notification.client.CourseServiceClient;
import com.lms.notification.client.IamServiceClient;
import com.lms.notification.dto.response.CourseBatchResponse;
import com.lms.notification.dto.response.LearnerInfoResponse;
import com.lms.notification.entity.Certificate;
import com.lms.notification.enums.NotificationType;
import com.lms.notification.repository.CertificateRepository; // Sửa tên Responsitory thành Repository
import com.lms.notification.service.CertificateService;
import com.lms.notification.service.NotificationService;
import com.lms.notification.service.PdfGeneratorService;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CertificateServiceImpl implements CertificateService {

    final CertificateRepository certificateRepository;
    final PdfGeneratorService pdfGeneratorService;
    final MinioClient minioClient;
    final IamServiceClient iamServiceClient;
    final CourseServiceClient courseServiceClient;
    final TemplateEngine templateEngine;
    final NotificationService notificationService;

    @Value("${minio.bucket-name}")
    String bucketName;

    @Value("${minio.uri}")
    String minioUri;

    @Override
    @Transactional
    public void generateCertificate(String learnerId, String courseId, String enrollmentId) {
        log.info("Bắt đầu quy trình sinh chứng chỉ cho Enrollment: {}", enrollmentId);

        // 1. Kiểm tra xem đã có chứng chỉ cho Enrollment này chưa (Tránh sinh trùng)
        if (certificateRepository.existsByEnrollmentId(enrollmentId)) {
            log.warn("Chứng chỉ cho Enrollment [{}] đã tồn tại. Bỏ qua.", enrollmentId);
            return;
        }

        try {
            // 2. Gọi API nội bộ lấy thông tin học viên & khóa học qua Feign Client
            log.info("Gọi Feign Client lấy thông tin học viên và khóa học...");
            ApiResponse<List<LearnerInfoResponse>> userResponse = iamServiceClient.getUserByIds(List.of(learnerId));
            ApiResponse<List<CourseBatchResponse>> courseResponse = courseServiceClient
                    .getCourseBatch(List.of(courseId));

            if (userResponse == null || userResponse.getData() == null || userResponse.getData().isEmpty()) {
                throw new RuntimeException("Không tìm thấy thông tin học viên từ iam-service");
            }
            if (courseResponse == null || courseResponse.getData() == null || courseResponse.getData().isEmpty()) {
                throw new RuntimeException("Không tìm thấy thông tin khóa học từ course-service");
            }

            LearnerInfoResponse learner = userResponse.getData().get(0);
            CourseBatchResponse course = courseResponse.getData().get(0);

            // 3. Tạo mã QR Code xác thực dạng Base64 chứa URL kiểm tra chứng chỉ
            String qrCodeHash = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(UUID.randomUUID().toString().getBytes());
            String verifyUrl = "http://localhost:8000/api/v1/certificates/verify/" + qrCodeHash;
            String qrCodeBase64 = generateQrCodeBase64(verifyUrl);

            // 4. Render HTML Template
            Context context = new Context();
            context.setVariable("learnerName", learner.getFullName());
            context.setVariable("courseTitle", course.getTitle());
            context.setVariable("qrCodeHash", qrCodeHash);
            context.setVariable("issuedAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            context.setVariable("qrCodeBase64", qrCodeBase64);

            String htmlContent = templateEngine.process("pdf/certificate", context);

            // 5. Sinh file PDF
            byte[] pdfBytes = pdfGeneratorService.generatePdfFromHtml(htmlContent);

            // 6. Upload file PDF lên MinIO
            String fileName = "certificates/" + UUID.randomUUID() + ".pdf";
            uploadPdfToMinio(fileName, pdfBytes);

            String pdfUrl = minioUri + "/" + bucketName + "/" + fileName;

            // 7. Lưu Certificate vào Database
            Certificate certificate = Certificate.builder()
                    .learnerId(learnerId)
                    .courseId(courseId)
                    .enrollmentId(enrollmentId)
                    .qrCodeHash(qrCodeHash)
                    .pdfUrl(pdfUrl)
                    .build();

            certificateRepository.save(certificate);
            log.info("Đã lưu chứng chỉ thành công vào DB cho học viên [{}]", learnerId);

            // 8. Đẩy thông báo chuông (SSE) cho học viên
            String title = "Chúc mừng bạn đã nhận được Chứng chỉ !";
            String notificationContent = "Bạn đã hoàn thành xuất sắc khóa học \"" + course.getTitle()
                    + "\" và nhận được chứng chỉ hoàn thành.";

            notificationService.createAndSendNotification(
                    learnerId,
                    title,
                    notificationContent,
                    NotificationType.COURSE_COMPLETED,
                    certificate.getId(),
                    "Certificate");

        } catch (Exception e) {
            log.error("❌ Thất bại khi sinh chứng chỉ cho Enrollment [{}]. Lỗi: {}", enrollmentId, e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Certificate> getMyCertificates(String learnerId) {
        return certificateRepository.findByLearnerId(learnerId);
    }

    @Override
    @Transactional(readOnly = true)
    public Certificate verifyCertificate(String qrCodeHash) {
        return certificateRepository.findByQrCodeHash(qrCodeHash)
                .orElseThrow(
                        () -> new IllegalArgumentException("Mã xác thực chứng chỉ không tồn tại hoặc không hợp lệ."));
    }

    // --- Helper sinh QR Code dạng Base64 ---
    private String generateQrCodeBase64(String text) throws Exception {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, 200, 200);
        try (ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream()) {
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            byte[] pngData = pngOutputStream.toByteArray();
            return Base64.getEncoder().encodeToString(pngData);
        }
    }

    // --- Helper upload file PDF lên MinIO ---
    private void uploadPdfToMinio(String fileName, byte[] content) throws Exception {
        boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!found) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
        try (ByteArrayInputStream bais = new ByteArrayInputStream(content)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(bais, content.length, -1)
                            .contentType("application/pdf")
                            .build());
            log.info("Đã upload chứng chỉ thành công lên MinIO với tên file: {}", fileName);
        }
    }
}
