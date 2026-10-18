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

            // Cấu hình kích thước trang A4 Landscape (297mm x 210mm)
            builder.useDefaultPageSize(297, 210, PdfRendererBuilder.PageSizeUnits.MM);

            // Đăng ký font Roboto hỗ trợ tiếng Việt UTF-8
            builder.useFont(() -> PdfGeneratorService.class.getResourceAsStream("/fonts/Roboto-Regular.ttf"), "Roboto",
                    400, PdfRendererBuilder.FontStyle.NORMAL, true);
            builder.useFont(() -> PdfGeneratorService.class.getResourceAsStream("/fonts/Roboto-Bold.ttf"), "Roboto",
                    700, PdfRendererBuilder.FontStyle.NORMAL, true);
            builder.useFont(() -> PdfGeneratorService.class.getResourceAsStream("/fonts/Roboto-Italic.ttf"), "Roboto",
                    400, PdfRendererBuilder.FontStyle.ITALIC, true);
            builder.useFont(() -> PdfGeneratorService.class.getResourceAsStream("/fonts/Roboto-BoldItalic.ttf"),
                    "Roboto", 700, PdfRendererBuilder.FontStyle.ITALIC, true);

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
