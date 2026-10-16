-- ============================================================
-- V2: Lesson Comments & Q&A
-- ============================================================

CREATE TABLE lesson_comments (
    id                  VARCHAR(36)     NOT NULL,
    lesson_id           VARCHAR(36)     NOT NULL    COMMENT 'Logical ID sang Course Service',
    course_id           VARCHAR(36)     NOT NULL    COMMENT 'Denormalized để truy vấn theo khóa học',
    user_id             VARCHAR(36)     NOT NULL    COMMENT 'ID người bình luận (từ IAM)',
    user_name           VARCHAR(255)    NULL        COMMENT 'Tên hiển thị người bình luận',
    user_avatar         VARCHAR(500)    NULL        COMMENT 'Avatar người bình luận',
    parent_id           VARCHAR(36)     NULL        COMMENT 'ID comment cha nếu là trả lời (reply)',
    content             TEXT            NOT NULL    COMMENT 'Nội dung bình luận/câu hỏi',
    likes_count         INT             NOT NULL    DEFAULT 0 COMMENT 'Số lượt thích',

    created_at          DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by          VARCHAR(36)     NULL,
    updated_by          VARCHAR(36)     NULL,
    is_deleted          TINYINT(1)      NOT NULL    DEFAULT 0,
    version             BIGINT          NOT NULL    DEFAULT 0,

    PRIMARY KEY (id),

    INDEX idx_comment_lesson        (lesson_id, created_at DESC),
    INDEX idx_comment_course        (course_id),
    INDEX idx_comment_parent        (parent_id),
    INDEX idx_comment_user          (user_id)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Bình luận và thảo luận hỏi đáp (Q&A) theo từng bài học';
