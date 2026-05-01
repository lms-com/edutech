CREATE TABLE IF NOT EXISTS media_files (
    id VARCHAR(36) PRIMARY KEY,
    original_file_name VARCHAR(255) NOT NULL,
    stored_file_name VARCHAR(255) NOT NULL,
    bucket_name VARCHAR(100) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    uploaded_by VARCHAR(36) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX idx_media_status ON media_files(status);
CREATE INDEX idx_media_uploaded_by ON media_files(uploaded_by);