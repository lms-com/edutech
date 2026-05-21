-- ============================================================
-- V2: Instructor Wallet & Double-Entry Ledger
-- Thiết kế theo chuẩn Double-entry Bookkeeping
--
-- Nguyên tắc cốt lõi:
--   available_balance + blocked_balance = total_balance
--   Mọi thay đổi số dư PHẢI có 1 dòng log trong balance_histories
--   Nếu cộng/trừ toàn bộ balance_histories theo instructor_id
--   → phải ra đúng available_balance hiện tại
-- ============================================================

-- Ví điện tử của từng Instructor
-- Mỗi Instructor có đúng 1 bản ghi trong bảng này
CREATE TABLE instructor_balances (
    id                  VARCHAR(36)     NOT NULL,

    instructor_id       VARCHAR(36)     NOT NULL        COMMENT 'Logical ID sang IAM Service — UNIQUE',

    currency_code       VARCHAR(10)     NOT NULL DEFAULT 'VND',

    -- available: có thể rút ngay
    available_balance   BIGINT          NOT NULL DEFAULT 0
                        COMMENT 'Số dư có thể rút. Đơn vị: VND (không dùng DECIMAL để tránh lỗi làm tròn)',

    -- blocked: đang chờ Admin duyệt rút, chưa thực sự mất
    blocked_balance     BIGINT          NOT NULL DEFAULT 0
                        COMMENT 'Số tiền đang bị đóng băng chờ duyệt payout',

    -- Kiểm tra ràng buộc: không được âm
    CONSTRAINT chk_available_non_negative CHECK (available_balance >= 0),
    CONSTRAINT chk_blocked_non_negative   CHECK (blocked_balance >= 0),

    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by          VARCHAR(36)     NULL,
    updated_by          VARCHAR(36)     NULL,
    is_deleted          TINYINT(1)      NOT NULL DEFAULT 0,
    version             BIGINT          NOT NULL DEFAULT 0,  -- Optimistic lock: QUAN TRỌNG khi cập nhật số dư đồng thời

    PRIMARY KEY (id),
    UNIQUE KEY uk_instructor_balances_instructor_id (instructor_id)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Ví điện tử của Instructor. Mỗi instructor có đúng 1 bản ghi';


-- Sổ cái: mọi biến động số dư đều ghi vào đây
-- Đây là nguồn sự thật (source of truth) — có thể replay lại để tính số dư bất kỳ thời điểm
CREATE TABLE balance_histories (
    id                  VARCHAR(36)     NOT NULL,

    instructor_id       VARCHAR(36)     NOT NULL        COMMENT 'Logical ID sang IAM Service',

    -- Loại giao dịch — mở rộng khi cần thêm loại mới
    transaction_type    ENUM(
                            'DEPOSIT_FROM_ORDER',   -- Cộng tiền sau khi học viên mua khóa học
                            'BLOCK_FOR_PAYOUT',     -- Khóa tiền khi tạo lệnh rút
                            'WITHDRAW_SUCCESS',     -- Trừ hẳn sau khi Admin duyệt rút
                            'WITHDRAW_REJECTED',    -- Hoàn tiền khi Admin từ chối rút
                            'REFUND_DEDUCTION'      -- Trừ tiền khi đơn hàng bị hoàn
                        ) NOT NULL,

    -- Số tiền biến động: DƯƠNG = cộng vào, ÂM = trừ ra
    -- Ví dụ: +5000000 khi nhận hoa hồng, -2000000 khi rút tiền
    amount              BIGINT          NOT NULL        COMMENT 'Dương = cộng, Âm = trừ',

    currency_code       VARCHAR(10)     NOT NULL DEFAULT 'VND',

    -- Snapshot số dư TẠI THỜI ĐIỂM giao dịch này xảy ra
    -- Để sau này có thể render lịch sử "Số dư trước → Số dư sau" cho Instructor
    balance_before      BIGINT          NOT NULL        COMMENT 'available_balance TRƯỚC khi áp dụng giao dịch này',
    balance_after       BIGINT          NOT NULL        COMMENT 'available_balance SAU khi áp dụng giao dịch này',

    -- Liên kết đến nguồn gốc giao dịch (để trace back)
    -- Chỉ 1 trong các trường này có giá trị, tùy transaction_type
    reference_id        VARCHAR(36)     NULL            COMMENT 'ID nguồn: revenue_share_id / payout_request_id / payment_id',
    reference_type      VARCHAR(50)     NULL            COMMENT 'Loại nguồn: REVENUE_SHARE / PAYOUT / PAYMENT',

    -- Ghi chú thêm nếu cần (VD: Admin ghi lý do từ chối)
    note                VARCHAR(500)    NULL,

    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by          VARCHAR(36)     NULL,
    updated_by          VARCHAR(36)     NULL,
    is_deleted          TINYINT(1)      NOT NULL DEFAULT 0,
    version             BIGINT          NOT NULL DEFAULT 0,

    PRIMARY KEY (id),

    -- Instructor xem lịch sử của mình (filter + sort)
    INDEX idx_balance_histories_instructor      (instructor_id, created_at DESC),
    INDEX idx_balance_histories_type            (instructor_id, transaction_type),
    INDEX idx_balance_histories_reference       (reference_id, reference_type)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Sổ cái — mọi biến động số dư của Instructor. Source of truth để audit và replay';
