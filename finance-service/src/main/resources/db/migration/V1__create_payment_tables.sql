-- ============================================================
-- V1: Payment Gateway Tables
-- ============================================================

CREATE TABLE payments (
    id                  VARCHAR(36)     NOT NULL,
    order_id            VARCHAR(36)     NOT NULL        COMMENT 'Logical ID sang Order Service',
    learner_id          VARCHAR(36)     NOT NULL        COMMENT 'Logical ID sang IAM Service',
    amount              BIGINT          NOT NULL        COMMENT 'Đơn vị VND — không dùng DECIMAL tránh lỗi float',
    currency_code       VARCHAR(10)     NOT NULL DEFAULT 'VND',

    -- FIX 1: VARCHAR thay ENUM — dễ thêm MOMO, ZALOPAY sau mà không cần ALTER TABLE
    payment_method      VARCHAR(20)     NOT NULL        COMMENT 'VNPAY | STRIPE | MOMO | ZALOPAY...',

    status              ENUM('PROCESSING','SUCCESS','FAILED','REFUNDED')
                        NOT NULL DEFAULT 'PROCESSING',
    return_url          VARCHAR(500)    NULL,
    paid_at             DATETIME        NULL            COMMENT 'Thời điểm ngân hàng xác nhận thành công',

    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by          VARCHAR(36)     NULL,
    updated_by          VARCHAR(36)     NULL,
    is_deleted          TINYINT(1)      NOT NULL DEFAULT 0,
    version             BIGINT          NOT NULL DEFAULT 0,

    PRIMARY KEY (id),
    INDEX idx_payments_order_id         (order_id),
    INDEX idx_payments_learner_status   (learner_id, status),
    INDEX idx_payments_status           (status)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Mỗi lần học viên khởi tạo thanh toán = 1 bản ghi';


CREATE TABLE payment_transactions (
    id                      VARCHAR(36)     NOT NULL,
    payment_id              VARCHAR(36)     NOT NULL,
    gateway_transaction_id  VARCHAR(100)    NOT NULL    COMMENT 'VNPay: vnp_TransactionNo | Stripe: charge_id',

    -- FIX 1: VARCHAR thay ENUM — đồng bộ với payments.payment_method
    gateway                 VARCHAR(20)     NOT NULL    COMMENT 'VNPAY | STRIPE | MOMO...',

    gateway_status          VARCHAR(50)     NOT NULL    COMMENT 'Mã gốc từ ngân hàng: 00=OK, 99=FAIL...',
    gateway_response        JSON            NULL        COMMENT 'Raw payload từ webhook — giữ nguyên để debug',
    amount                  BIGINT          NOT NULL,
    currency_code           VARCHAR(10)     NOT NULL DEFAULT 'VND',
    transacted_at           DATETIME        NULL,

    created_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by              VARCHAR(36)     NULL,
    updated_by              VARCHAR(36)     NULL,
    is_deleted              TINYINT(1)      NOT NULL DEFAULT 0,
    version                 BIGINT          NOT NULL DEFAULT 0,

    PRIMARY KEY (id),

    -- KEY của idempotency: cùng gateway + cùng transactionId không được xử lý 2 lần
    UNIQUE KEY uk_gateway_transaction (gateway, gateway_transaction_id),

    INDEX idx_payment_transactions_payment_id (payment_id),

    CONSTRAINT fk_pt_payment FOREIGN KEY (payment_id) REFERENCES payments(id)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Log thô từng webhook ngân hàng. uk_gateway_transaction là idempotency key';
