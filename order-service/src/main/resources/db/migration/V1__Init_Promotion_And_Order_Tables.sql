CREATE TABLE IF NOT EXISTS promotions (
    id VARCHAR(36) PRIMARY KEY,
    code VARCHAR(20) UNIQUE NOT NULL,
    discount_percent DECIMAL(5, 2),
    discount_amount DECIMAL(15, 2),
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    usage_limit INT DEFAULT NULL,
    usage_count INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    updated_by VARCHAR(36),
    version BIGINT DEFAULT 0
);


CREATE TABLE IF NOT EXISTS course_promotions (
    id VARCHAR(36) PRIMARY KEY,
    promotion_id VARCHAR(36) NOT NULL,
    course_id VARCHAR(36) NOT NULL,

    CONSTRAINT fk_course_pro_pro FOREIGN KEY (promotion_id) REFERENCES promotions(id),
    UNIQUE INDEX idx_pro_course (promotion_id, course_id)
);


CREATE TABLE IF NOT EXISTS orders (
    id VARCHAR(36) PRIMARY KEY,
    learner_id VARCHAR(36) NOT NULL,
    total_price DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    currency_code VARCHAR(3) NOT NULL DEFAULT 'VND',
    exchange_rate DECIMAL(12, 6) NOT NULL DEFAULT 1.000000,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    updated_by VARCHAR(36),
    version BIGINT DEFAULT 0,

    INDEX idx_learner (learner_id),
    INDEX idx_status (status)
);


CREATE TABLE IF NOT EXISTS order_details (
    id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    course_id VARCHAR(36) NOT NULL,
    course_name VARCHAR(255) NOT NULL,
    instructor_id VARCHAR(36) NOT NULL,
    promotion_id VARCHAR(36),
    price_at_purchase DECIMAL(15,2) NOT NULL,
    discount_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    final_price DECIMAL(15, 2) NOT NULL,

    CONSTRAINT fk_order_details_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_order_details_pro FOREIGN KEY (promotion_id) REFERENCES promotions(id),
    INDEX idx_course (course_id)
);