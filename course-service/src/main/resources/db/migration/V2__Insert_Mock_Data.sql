-- V2__Insert_Mock_Data.sql
-- Thêm dữ liệu mẫu cho hệ thống quản lý khóa học (Course Service)

-- ==========================================
-- 1. Thêm Danh mục (Categories)
-- ==========================================
-- Danh mục gốc: Lập trình (ID: c_lap_trinh)
INSERT INTO categories (id, name, slug, parent_id, order_index, is_deleted, version)
VALUES ('c_lap_trinh', 'Lập trình', 'lap-trinh', NULL, 0, FALSE, 0);

-- Danh mục gốc: Thiết kế (ID: c_thiet_ke)
INSERT INTO categories (id, name, slug, parent_id, order_index, is_deleted, version)
VALUES ('c_thiet_ke', 'Thiết kế', 'thiet-ke', NULL, 1, FALSE, 0);

-- Danh mục con: Lập trình Java (Thuộc Lập trình)
INSERT INTO categories (id, name, slug, parent_id, order_index, is_deleted, version)
VALUES ('c_java', 'Java', 'java', 'c_lap_trinh', 0, FALSE, 0);

-- Danh mục con: Lập trình Web (Thuộc Lập trình)
INSERT INTO categories (id, name, slug, parent_id, order_index, is_deleted, version)
VALUES ('c_web', 'Web Development', 'web-development', 'c_lap_trinh', 1, FALSE, 0);

-- ==========================================
-- 2. Thêm Khóa học (Courses)
-- Ghi chú: instructor_id được giả lập trùng khớp với Admin/Instructor bên IAM Service
-- ==========================================
-- Khóa học 1 (Java Spring Boot - PUBLISHED)
INSERT INTO courses (id, category_id, instructor_id, title, slug, description, thumbnail_url, level, base_price, currency_code, status, override_commission_rate, is_deleted, version)
VALUES (
  'course_spring_boot', 'c_java', 'uuid-instructor-test-123',
  'Làm chủ Spring Boot 3x từ A-Z', 'lam-chu-spring-boot-3',
  'Khóa học thực chiến xây dựng hệ thống web với Spring Boot 3, Hibernate, Security, JWT...',
  'https://example.com/spring-boot-thumb.jpg', 'INTERMEDIATE', 1500000, 'VND', 'PUBLISHED', 0.85, FALSE, 0
);

-- Khóa học 2 (Frontend React - PENDING)
INSERT INTO courses (id, category_id, instructor_id, title, slug, description, thumbnail_url, level, base_price, currency_code, status, override_commission_rate, is_deleted, version)
VALUES (
  'course_react', 'c_web', 'uuid-instructor-test-123',
  'Frontend ReactJS Thực chiến', 'reactjs-thuc-chien',
  'Xây dựng ứng dụng web Single Page Application (SPA) hiệu suất cao với React 18, Redux Toolkit.',
  'https://example.com/react-thumb.jpg', 'BEGINNER', 1200000, 'VND', 'PENDING', NULL, FALSE, 0
);

-- ==========================================
-- 3. Thêm Chương học (Sections) cho khóa học Spring Boot
-- ==========================================
INSERT INTO sections (id, course_id, title, order_index, is_deleted, version)
VALUES ('section_1_sb', 'course_spring_boot', 'Chương 1: Mở đầu', 0, FALSE, 0);

INSERT INTO sections (id, course_id, title, order_index, is_deleted, version)
VALUES ('section_2_sb', 'course_spring_boot', 'Chương 2: Spring Core và DI/IoC', 1, FALSE, 0);

-- ==========================================
-- 4. Thêm Bài học (Lessons) - Dùng Đa hình (Inheritance)
-- ==========================================

-- Bài 1: Video (Thuộc Chương 1)
INSERT INTO lessons (id, section_id, title, type, order_index, is_free_preview, is_deleted, version)
VALUES ('lesson_1_video', 'section_1_sb', 'Cài đặt môi trường Java & IntelliJ', 'VIDEO', 0, TRUE, FALSE, 0);
-- Thông tin riêng của Video
INSERT INTO video_lessons (lesson_id, video_url, duration)
VALUES ('lesson_1_video', 'https://example.com/video1.mp4', 600);


-- Bài 2: Video (Thuộc Chương 1)
INSERT INTO lessons (id, section_id, title, type, order_index, is_free_preview, is_deleted, version)
VALUES ('lesson_2_video', 'section_1_sb', 'Khởi tạo dự án Spring Boot đầu tiên', 'VIDEO', 1, FALSE, FALSE, 0);
-- Thông tin riêng của Video
INSERT INTO video_lessons (lesson_id, video_url, duration)
VALUES ('lesson_2_video', 'https://example.com/video2.mp4', 1200);


-- Bài 3: Quiz (Thuộc Chương 1)
INSERT INTO lessons (id, section_id, title, type, order_index, is_free_preview, is_deleted, version)
VALUES ('lesson_3_quiz', 'section_1_sb', 'Trắc nghiệm Chương 1', 'QUIZ', 2, FALSE, FALSE, 0);
-- Thông tin riêng của Quiz
INSERT INTO quizzes (lesson_id, pass_score)
VALUES ('lesson_3_quiz', 80.00);


-- ==========================================
-- 5. Thêm Câu hỏi và Đáp án cho Bài Quiz (ID: lesson_3_quiz)
-- ==========================================

-- Câu hỏi 1
INSERT INTO questions (id, quiz_id, question_text, order_index, is_deleted, version)
VALUES ('q1', 'lesson_3_quiz', 'Framework Spring Boot được phát triển bằng ngôn ngữ lập trình nào?', 0, FALSE, 0);

-- Đáp án cho Câu 1
INSERT INTO answers (id, question_id, option_text, is_correct, is_deleted, version) VALUES ('a1_1', 'q1', 'Python', FALSE, FALSE, 0);
INSERT INTO answers (id, question_id, option_text, is_correct, is_deleted, version) VALUES ('a1_2', 'q1', 'Java', TRUE, FALSE, 0);
INSERT INTO answers (id, question_id, option_text, is_correct, is_deleted, version) VALUES ('a1_3', 'q1', 'C#', FALSE, FALSE, 0);
INSERT INTO answers (id, question_id, option_text, is_correct, is_deleted, version) VALUES ('a1_4', 'q1', 'JavaScript', FALSE, FALSE, 0);


-- Câu hỏi 2
INSERT INTO questions (id, quiz_id, question_text, order_index, is_deleted, version)
VALUES ('q2', 'lesson_3_quiz', 'Điểm mạnh nhất của Spring Boot so với Spring Framework truyền thống là gì?', 1, FALSE, 0);

-- Đáp án cho Câu 2
INSERT INTO answers (id, question_id, option_text, is_correct, is_deleted, version) VALUES ('a2_1', 'q2', 'Chạy nhanh hơn gấp 10 lần', FALSE, FALSE, 0);
INSERT INTO answers (id, question_id, option_text, is_correct, is_deleted, version) VALUES ('a2_2', 'q2', 'Auto-configuration (Cấu hình tự động) giúp giảm bớt cấu hình XML/Java', TRUE, FALSE, 0);
INSERT INTO answers (id, question_id, option_text, is_correct, is_deleted, version) VALUES ('a2_3', 'q2', 'Tích hợp sẵn hệ quản trị cơ sở dữ liệu riêng', FALSE, FALSE, 0);