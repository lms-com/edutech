-- =====================================================================
-- V4: Bổ sung thời lượng làm bài (duration) cho bảng quizzes
-- và giải thích đáp án (explanation) cho bảng questions
-- =====================================================================

ALTER TABLE quizzes
    ADD COLUMN duration INT NOT NULL DEFAULT 900 COMMENT 'Thời lượng làm bài tính bằng giây (mặc định 900s = 15 phút)';

ALTER TABLE questions
    ADD COLUMN explanation TEXT NULL COMMENT 'Giải thích chi tiết đáp án đúng sau khi nộp bài';

-- Cập nhật lời giải thích cho các câu hỏi mẫu
UPDATE questions
SET explanation = 'Spring Boot là một nhánh mở rộng thuộc hệ sinh thái Spring Framework và được viết hoàn toàn bằng ngôn ngữ lập trình Java (chạy trên máy ảo JVM).'
WHERE id = 'q1';

UPDATE questions
SET explanation = 'Spring Boot nổi bật với tính năng Auto-configuration (Tự động cấu hình) và Embedded Server (Tomcat tích hợp sẵn), giúp lập trình viên khởi tạo ứng dụng nhanh chóng mà không cần cấu hình XML phức tạp.'
WHERE id = 'q2';
