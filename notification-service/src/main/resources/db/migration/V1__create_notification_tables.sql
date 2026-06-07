

CREATE TABLE notifications (

                               id              VARCHAR(36)     NOT NULL,
                               user_id         VARCHAR(36)     NOT NULL    COMMENT 'Logical ID sang IAM Service',

    -- Loại thông báo để Frontend render icon đúng
                               type            VARCHAR(50)     NOT NULL    COMMENT 'ORDER_COMPLETED | COURSE_APPROVED | COURSE_REJECTED | COURSE_COMPLETED | OTP_SENT',

                               title           VARCHAR(255)    NOT NULL,
                               content         TEXT            NULL,

                               is_read         TINYINT(1)      NOT NULL    DEFAULT 0,
                               read_at         DATETIME        NULL,

    -- Liên kết đến nguồn gốc thông báo (để Frontend điều hướng khi click)
                               reference_id    VARCHAR(36)     NULL        COMMENT 'courseId | orderId | enrollmentId...',
                               reference_type  VARCHAR(50)     NULL        COMMENT 'COURSE | ORDER | ENROLLMENT',

                               created_at      DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP,
                               updated_at      DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                               created_by      VARCHAR(36)     NULL,
                               updated_by      VARCHAR(36)     NULL,
                               is_deleted      TINYINT(1)      NOT NULL    DEFAULT 0,
                               version         BIGINT          NOT NULL    DEFAULT 0,

                               PRIMARY KEY (id),

    -- Query chính: user xem thông báo của mình, sort mới nhất trước
                               INDEX idx_notifications_user        (user_id, created_at DESC),

    -- Filter theo trạng thái đọc (đếm số chưa đọc cho quả chuông)
                               INDEX idx_notifications_user_read   (user_id, is_read)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='In-app notifications. reference_id/type để Frontend điều hướng khi click';


CREATE TABLE email_logs (
                            id              VARCHAR(36)     NOT NULL,
                            recipient_email VARCHAR(255)    NOT NULL,
                            subject         VARCHAR(500)    NOT NULL,
                            template_name   VARCHAR(100)    NOT NULL    COMMENT 'Tên template HTML đã dùng',
                            status          ENUM('SENT','FAILED') NOT NULL DEFAULT 'SENT',
                            error_message   TEXT            NULL        COMMENT 'Lỗi nếu gửi thất bại',
                            sent_at         DATETIME        NULL,

    -- Liên kết đến event gốc để audit
                            reference_id    VARCHAR(36)     NULL,
                            reference_type  VARCHAR(50)     NULL,

                            created_at      DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP,
                            updated_at      DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                            created_by      VARCHAR(36)     NULL,
                            updated_by      VARCHAR(36)     NULL,
                            is_deleted      TINYINT(1)      NOT NULL    DEFAULT 0,
                            version         BIGINT          NOT NULL    DEFAULT 0,

                            PRIMARY KEY (id),
                            INDEX idx_email_logs_recipient  (recipient_email, created_at DESC),
                            INDEX idx_email_logs_status     (status)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Log mọi email đã gửi — audit trail và debug khi email không đến';


CREATE TABLE certificates (
                              id              VARCHAR(36)     NOT NULL,
                              learner_id      VARCHAR(36)     NOT NULL    COMMENT 'Logical ID sang IAM Service',
                              course_id       VARCHAR(36)     NOT NULL    COMMENT 'Logical ID sang Course Service',
                              enrollment_id   VARCHAR(36)     NOT NULL    COMMENT 'Logical ID sang Enrollment Service',

    -- Mã hash duy nhất — nhúng vào QR Code trên PDF
    -- Employer quét QR → GET /certificates/verify/{qrCodeHash}
                              qr_code_hash    VARCHAR(100)    NOT NULL,

    -- Đường dẫn file PDF trên MinIO
                              pdf_url         VARCHAR(500)    NOT NULL,

                              issued_at       DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP,

                              created_at      DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP,
                              updated_at      DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                              created_by      VARCHAR(36)     NULL,
                              updated_by      VARCHAR(36)     NULL,
                              is_deleted      TINYINT(1)      NOT NULL    DEFAULT 0,
                              version         BIGINT          NOT NULL    DEFAULT 0,

                              PRIMARY KEY (id),

    -- Employer verify QR — query thường xuyên nhất
                              UNIQUE KEY uk_certificate_qr_hash       (qr_code_hash),

    -- Chống cấp 2 lần cho cùng 1 enrollment
                              UNIQUE KEY uk_certificate_enrollment    (enrollment_id),

                              INDEX idx_certificate_learner   (learner_id),
                              INDEX idx_certificate_course    (course_id)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Chứng chỉ hoàn thành khóa học. UNIQUE(qr_code_hash) để verify nhanh. UNIQUE(enrollment_id) chống cấp 2 lần';