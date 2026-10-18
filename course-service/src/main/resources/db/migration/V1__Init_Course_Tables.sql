-- ==============================================================================
-- FLYWAY SCRIPT: V1__Init_Course_Tables.sql
-- MICROSERVICE: Catalog & Content Service (content_db)
-- ==============================================================================

-- 1. BẢNG CATEGORIES (Danh mục khóa học)
CREATE TABLE categories (
                            id VARCHAR(36) PRIMARY KEY,
                            name VARCHAR(255) NOT NULL,
                            slug VARCHAR(255) NOT NULL UNIQUE,
                            parent_id VARCHAR(36) DEFAULT NULL,
                            order_index INT NOT NULL DEFAULT 0,
                            is_deleted BOOLEAN DEFAULT FALSE,

    -- Audit Columns
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                            created_by VARCHAR(36),
                            updated_by VARCHAR(36),
                            version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_category_parent ON categories(parent_id);

-- 2. BẢNG COURSES (Khóa học)
CREATE TABLE courses (
                         id VARCHAR(36) PRIMARY KEY,
                         category_id VARCHAR(36) NOT NULL,
                         instructor_id VARCHAR(36) NOT NULL, -- Logical ID trỏ sang iam_db.users
                         title VARCHAR(255) NOT NULL,
                         slug VARCHAR(255) NOT NULL UNIQUE,
                         description TEXT,
                         thumbnail_url VARCHAR(500),
                         level VARCHAR(50), -- VD: BEGINNER, INTERMEDIATE, ADVANCED
                         base_price DECIMAL(15, 2) NOT NULL DEFAULT 0,
                         currency_code VARCHAR(10) NOT NULL DEFAULT 'VND',
                         status VARCHAR(50) NOT NULL DEFAULT 'DRAFT', -- DRAFT, PENDING, PUBLISHED, ARCHIVED
                         override_commission_rate DECIMAL(3, 2) DEFAULT NULL, -- Ghi đè hoa hồng (VD: 0.85 = 85%)
                         is_deleted BOOLEAN DEFAULT FALSE,

    -- Audit Columns
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                         created_by VARCHAR(36),
                         updated_by VARCHAR(36),
                         version BIGINT NOT NULL DEFAULT 0,

                         CONSTRAINT fk_course_category FOREIGN KEY (category_id) REFERENCES categories(id)
);
CREATE INDEX idx_course_instructor ON courses(instructor_id);
CREATE INDEX idx_course_status ON courses(status);

-- 3. BẢNG SECTIONS (Chương học)
CREATE TABLE sections (
                          id VARCHAR(36) PRIMARY KEY,
                          course_id VARCHAR(36) NOT NULL,
                          title VARCHAR(255) NOT NULL,
                          order_index INT NOT NULL DEFAULT 0,
                          is_deleted BOOLEAN DEFAULT FALSE, -- ĐÃ BỔ SUNG

    -- Audit Columns
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                          created_by VARCHAR(36),
                          updated_by VARCHAR(36),
                          version BIGINT NOT NULL DEFAULT 0,

                          CONSTRAINT fk_section_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
);

-- 4. BẢNG LESSONS (Bài học - Bảng cha)
CREATE TABLE lessons (
                         id VARCHAR(36) PRIMARY KEY,
                         section_id VARCHAR(36) NOT NULL,
                         title VARCHAR(255) NOT NULL,
                         type VARCHAR(20) NOT NULL, -- VIDEO, QUIZ, DOC
                         order_index INT NOT NULL DEFAULT 0,
                         is_free_preview BOOLEAN DEFAULT FALSE,
                         is_deleted BOOLEAN DEFAULT FALSE, -- ĐÃ BỔ SUNG

    -- Audit Columns
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                         created_by VARCHAR(36),
                         updated_by VARCHAR(36),
                         version BIGINT NOT NULL DEFAULT 0,

                         CONSTRAINT fk_lesson_section FOREIGN KEY (section_id) REFERENCES sections(id) ON DELETE CASCADE
);

-- 5. BẢNG VIDEO_LESSONS (Nội dung Bài học Video - 1:1 với Lessons)
CREATE TABLE video_lessons (
                               lesson_id VARCHAR(36) PRIMARY KEY,
                               video_url VARCHAR(500) NOT NULL, -- Path lưu trên MinIO (VD: /bucket/file.m3u8)
                               duration INT NOT NULL DEFAULT 0, -- Thời lượng tính bằng giây

                               CONSTRAINT fk_videolesson_lesson FOREIGN KEY (lesson_id) REFERENCES lessons(id) ON DELETE CASCADE
);

-- 6. BẢNG QUIZZES (Nội dung Bài kiểm tra - 1:1 với Lessons)
CREATE TABLE quizzes (
                         lesson_id VARCHAR(36) PRIMARY KEY,
                         pass_score DECIMAL(5, 2) NOT NULL DEFAULT 0, -- Điểm đỗ (VD: 80.00)

                         CONSTRAINT fk_quiz_lesson FOREIGN KEY (lesson_id) REFERENCES lessons(id) ON DELETE CASCADE
);

-- 7. BẢNG QUESTIONS (Câu hỏi trắc nghiệm)
CREATE TABLE questions (
                           id VARCHAR(36) PRIMARY KEY,
                           quiz_id VARCHAR(36) NOT NULL,
                           question_text TEXT NOT NULL,
                           order_index INT NOT NULL DEFAULT 0,
                           is_deleted BOOLEAN DEFAULT FALSE, -- ĐÃ BỔ SUNG

    -- Audit Columns
                           created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                           created_by VARCHAR(36),
                           updated_by VARCHAR(36),
                           version BIGINT NOT NULL DEFAULT 0,

                           CONSTRAINT fk_question_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes(lesson_id) ON DELETE CASCADE
);

-- 8. BẢNG ANSWERS (Đáp án)
CREATE TABLE answers (
                         id VARCHAR(36) PRIMARY KEY,
                         question_id VARCHAR(36) NOT NULL,
                         option_text TEXT NOT NULL,
                         is_correct BOOLEAN NOT NULL DEFAULT FALSE,
                         is_deleted BOOLEAN DEFAULT FALSE, -- ĐÃ BỔ SUNG

    -- Audit Columns
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                         created_by VARCHAR(36),
                         updated_by VARCHAR(36),
                         version BIGINT NOT NULL DEFAULT 0,

                         CONSTRAINT fk_answer_question FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE
);