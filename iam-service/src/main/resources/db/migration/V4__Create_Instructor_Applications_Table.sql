CREATE TABLE IF NOT EXISTS instructor_applications (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    bio TEXT NOT NULL,
    expertise VARCHAR(255) NOT NULL,
    experience_years INT DEFAULT 0,
    portfolio_url VARCHAR(500),
    cv_url VARCHAR(500),
    status ENUM('PENDING', 'APPROVED', 'REJECTED') DEFAULT 'PENDING',
    rejection_reason TEXT,
    reviewed_by VARCHAR(36),
    reviewed_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_inst_app_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_inst_app_user ON instructor_applications(user_id);
CREATE INDEX idx_inst_app_status ON instructor_applications(status);
