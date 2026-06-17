package com.lms.notification.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
@Slf4j
public class PdfGeneratorService {

    public byte[] generatePdfFromHtml(String htmlContent) throws Exception {
        log.info("Bắt đầu sinh PDF từ chuỗi HTML...");
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(htmlContent, "/");
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception e) {
            log.error("Lỗi khi sinh PDF: {}", e.getMessage(), e);
            throw e;
        }
    }
}
