package com.lms.notification.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Chứng chỉ trả về cho giao diện, kèm tên học viên và tên khóa học.
 *
 * Trước đây hai endpoint trả thẳng entity nên payload chỉ có ID. Hệ quả: trang xác
 * thực công khai không cho biết ai được cấp và khóa học nào — gần như chỉ còn là
 * máy kiểm tra chuỗi băm — còn bản chứng chỉ hiển thị các dòng rỗng.
 */
@Data
@Builder
public class CertificateResponse {
    String id;
    String learnerId;
    String learnerName;
    String courseId;
    String courseTitle;
    String enrollmentId;
    String qrCodeHash;
    String pdfUrl;
    LocalDateTime issuedAt;
}
