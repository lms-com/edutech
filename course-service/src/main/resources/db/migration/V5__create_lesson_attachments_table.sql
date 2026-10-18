-- Tạo bảng lưu trữ tài liệu đính kèm cho bài học (PDF, Slides, Mã nguồn, Tài liệu tham khảo)
CREATE TABLE IF NOT EXISTS lesson_attachments (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    lesson_id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    file_url VARCHAR(1000) NOT NULL,
    file_size VARCHAR(50) NULL,
    file_type VARCHAR(50) NULL,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_lesson_attachments_lesson FOREIGN KEY (lesson_id) REFERENCES lessons(id) ON DELETE CASCADE
);

CREATE INDEX idx_lesson_attachments_lesson_id ON lesson_attachments(lesson_id);
