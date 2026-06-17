package com.lms.notification;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lms.notification.service.PdfGeneratorService;
import org.junit.jupiter.api.Test;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

public class CertificatePreviewTest {

    @Test
    void previewCertificatePdf() throws Exception {
        // 1. Cấu hình SpringTemplateEngine chạy offline để sử dụng SpEL thay vì OGNL (tránh lỗi ClassNotFound ognl.ClassResolver)
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        templateEngine.setTemplateResolver(resolver);

        // 2. Sinh mã QR giả lập dạng Base64
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode("http://localhost:8000/api/v1/certificates/verify/demo-hash-code", BarcodeFormat.QR_CODE, 200, 200);
        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        String qrCodeBase64 = Base64.getEncoder().encodeToString(pngOutputStream.toByteArray());

        // 3. Đưa dữ liệu mẫu vào Context để hiển thị lên HTML
        Context context = new Context();
        context.setVariable("learnerName", "NGUYỄN VĂN HỌC VIÊN");
        context.setVariable("courseTitle", "Hệ Thống Microservices với Spring Boot & Spring Cloud");
        context.setVariable("qrCodeHash", "CERT-8888-9999-AAAA");
        context.setVariable("issuedAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        context.setVariable("qrCodeBase64", qrCodeBase64);

        // 4. Dựng chuỗi HTML hoàn chỉnh từ templates/pdf/certificate.html
        String htmlContent = templateEngine.process("pdf/certificate", context);

        // 5. Chuyển đổi HTML sang định dạng PDF
        PdfGeneratorService pdfGeneratorService = new PdfGeneratorService();
        byte[] pdfBytes = pdfGeneratorService.generatePdfFromHtml(htmlContent);

        // 6. Lưu file PDF xuống thư mục 'target/' của dự án
        String outputPath = "target/certificate-preview.pdf";
        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            fos.write(pdfBytes);
        }

        System.out.println("DA SINH FILE XEM TRUOC TAI: " + outputPath);
    }
}
