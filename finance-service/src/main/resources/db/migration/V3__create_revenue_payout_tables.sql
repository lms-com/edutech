-- ============================================================
-- V3: Revenue Share & Payout & Bank Account Tables
-- ============================================================

CREATE TABLE revenue_shares (
    id                  VARCHAR(36)     NOT NULL,
    order_id            VARCHAR(36)     NOT NULL        COMMENT 'Logical ID sang Order Service',
    course_id           VARCHAR(36)     NOT NULL        COMMENT 'Logical ID sang Course Service',
    instructor_id       VARCHAR(36)     NOT NULL        COMMENT 'Logical ID sang IAM Service',
    gross_amount        DECIMAL(15, 2)  NOT NULL        COMMENT 'Giá bán thực tế của khóa học trong đơn',
    currency_code       VARCHAR(3)      NOT NULL DEFAULT 'VND',
    commission_rate     DECIMAL(5,4)    NOT NULL        COMMENT 'Snapshot tỷ lệ lúc chia — VD: 0.7000 = 70%',
    instructor_amount   DECIMAL(15, 2)  NOT NULL        COMMENT 'gross_amount * commission_rate',
    platform_fee        DECIMAL(15, 2)  NOT NULL        COMMENT 'gross_amount - instructor_amount',
    status              VARCHAR(20)     NOT NULL DEFAULT 'HOLDING'
        COMMENT 'Trạng thái của tiền sau thanh toán: HOLDING, RELEASED, REFUNDED',
    original_revenue_id VARCHAR(36)     NULL            COMMENT 'Chỉ dùng cho revenue refund hoàn tiền để tham chiếu đến revenue gốc khi nhân tiện',
    idempotency_key     VARCHAR(100)    NOT NULL        COMMENT 'Cấu trúc: ORDER_ID:COURSE_ID:ACTION',

    CONSTRAINT chk_revenue_split CHECK (instructor_amount + platform_fee = gross_amount),

    created_at          TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),

    -- Chống xử lý event order.completed 2 lần cho cùng 1 khóa học
    -- Dòng gốc: original_revenue_id là NULL. Khóa sẽ là (ORD-1, CRS-1, NULL) --> Chống trùng event mua hàng.
    -- Dòng refund âm: original_revenue_id là REV-001. Khóa sẽ là (ORD-1, CRS-1, 'REV-001') --> Chống trùng event refund.

    INDEX idx_revenue_instructor        (instructor_id, created_at DESC),
    INDEX idx_revenue_course_id_status         (course_id, status),

    -- FIX 2: Index riêng cho Admin query platform_revenue theo tháng
    -- Admin query: GROUP BY YEAR(created_at), MONTH(created_at) trên toàn bảng
    INDEX idx_revenue_created_at        (created_at),
    -- Index phục vụ cho cron job quét kiểm tra tự động thời hạn 7 ngày của mỗi revenue share
    INDEX idx_revenue_cron_trigger (status, created_at),

    -- Duy nhất 1 Unique Key này là đủ cân cả hệ thống
    UNIQUE KEY uk_revenue_idempotency (idempotency_key)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Chia hoa hồng mỗi đơn hàng. UNIQUE(order_id,course_id) chống duplicate RabbitMQ event';


CREATE TABLE payout_requests (
    id                  VARCHAR(36)     NOT NULL,
    instructor_id       VARCHAR(36)     NOT NULL,
    amount              DECIMAL(15, 2)          NOT NULL,
    currency_code       VARCHAR(10)     NOT NULL DEFAULT 'VND',
    status              ENUM('PENDING','SUCCESS','REJECTED') NOT NULL DEFAULT 'PENDING',

    -- Snapshot thông tin ngân hàng TẠI THỜI ĐIỂM tạo lệnh
    -- Lý do: nếu Instructor đổi tài khoản sau khi tạo lệnh,
    -- lệnh cũ vẫn chuyển đúng tài khoản ban đầu
    bank_code           VARCHAR(20)     NOT NULL,
    account_number      VARCHAR(50)     NOT NULL,
    account_name        VARCHAR(200)    NOT NULL,

    processed_by        VARCHAR(36)     NULL            COMMENT 'Admin ID thực hiện duyệt/từ chối',
    processed_at        TIMESTAMP        NULL,
    reject_reason       VARCHAR(500)    NULL,
    bank_reference_no   VARCHAR(100)    NULL            COMMENT 'Số bút toán CK thực tế — Admin điền sau',

    created_at          TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version             BIGINT          NOT NULL DEFAULT 0      COMMENT 'Tránh tình trạng Admin nhấn double-click duyệt 1 lệnh 2 lần',

    PRIMARY KEY (id),
    INDEX idx_payout_instructor (instructor_id, created_at DESC),
    INDEX idx_payout_status     (status, created_at DESC)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Lệnh rút tiền. blocked_balance tăng lúc PENDING, giảm lúc SUCCESS hoặc REJECTED';


-- FIX 3: Thêm INDEX để enforce is_primary = 1 chỉ có 1 bản ghi mỗi instructor
-- Không thể dùng UNIQUE constraint thuần vì is_primary = 0 có many bản ghi
-- => Xử lý ở Service layer: unset tất cả trước, rồi mới set cái mới
CREATE TABLE bank_accounts (
    id                  VARCHAR(36)     NOT NULL,
    instructor_id       VARCHAR(36)     NOT NULL,
    bank_code           VARCHAR(20)     NOT NULL        COMMENT 'VCB | TCB | MB | ACB...',
    account_number      VARCHAR(50)     NOT NULL,
    account_name        VARCHAR(200)    NOT NULL        COMMENT 'Tên chủ TK — phải khớp chính xác tên ngân hàng',
    is_primary          TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '1 = tài khoản mặc định để rút tiền',

    created_at          TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    INDEX idx_bank_instructor           (instructor_id),

    -- Partial unique: chỉ enforce 1 primary PER instructor (xử lý ở tầng Service)
    -- DB không hỗ trợ partial unique natively trong MySQL
    -- => Comment để dev nhớ xử lý trong PayoutService.setBankAccountPrimary()
    INDEX idx_bank_instructor_primary   (instructor_id, is_primary)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Tài khoản ngân hàng của Instructor. is_primary duy nhất được enforce ở Service layer';
