CREATE TABLE IF NOT EXISTS refund_requests (
    id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    learner_id VARCHAR(36) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    currency_code VARCHAR(3) NOT NULL DEFAULT 'VND',
    reason VARCHAR(255) NOT NULL,
    detail TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    admin_note TEXT,
    resolved_by VARCHAR(36),
    resolved_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    updated_by VARCHAR(36),
    version BIGINT DEFAULT 0,

    CONSTRAINT fk_refund_order FOREIGN KEY (order_id) REFERENCES orders(id),
    INDEX idx_refund_order (order_id),
    INDEX idx_refund_learner (learner_id),
    INDEX idx_refund_status (status)
);
