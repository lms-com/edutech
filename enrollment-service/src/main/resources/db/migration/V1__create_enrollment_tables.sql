-- ============================================================
-- V1: Enrollment Service Tables
-- ============================================================

CREATE TABLE enrollments (
    id                  VARCHAR(36)     NOT NULL,
    learner_id          VARCHAR(36)     NOT NULL    COMMENT 'Logical ID sang IAM Service',
    course_id           VARCHAR(36)     NOT NULL    COMMENT 'Logical ID sang Course Service',
    status              ENUM('ACTIVE','REVOKED')    NOT NULL DEFAULT 'ACTIVE',
    started_at          DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP,
    completed_rate      INT             NOT NULL    DEFAULT 0 COMMENT '0-100 phần trăm',

    created_at          DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by          VARCHAR(36)     NULL,
    updated_by          VARCHAR(36)     NULL,
    is_deleted          TINYINT(1)      NOT NULL    DEFAULT 0,
    version             BIGINT          NOT NULL    DEFAULT 0,

    PRIMARY KEY (id),

    -- Idempotency: chống RabbitMQ deliver order.completed 2 lần
    UNIQUE KEY uk_enrollment_learner_course (learner_id, course_id),

    INDEX idx_enrollment_learner    (learner_id),
    INDEX idx_enrollment_course     (course_id),
    INDEX idx_enrollment_status     (status)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Quyền truy cập khóa học của học viên';


CREATE TABLE lesson_progresses (
    id                      VARCHAR(36)     NOT NULL,
    enrollment_id           VARCHAR(36)     NOT NULL,
    lesson_id               VARCHAR(36)     NOT NULL    COMMENT 'Logical ID sang Course Service',
    is_completed            TINYINT(1)      NOT NULL    DEFAULT 0,
    last_watch_time_seconds INT             NOT NULL    DEFAULT 0,

    created_at              DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by              VARCHAR(36)     NULL,
    updated_by              VARCHAR(36)     NULL,
    is_deleted              TINYINT(1)      NOT NULL    DEFAULT 0,
    version                 BIGINT          NOT NULL    DEFAULT 0,

    PRIMARY KEY (id),

    -- UPSERT cần key này: không có thì mỗi lần flush Redis tạo duplicate row
    UNIQUE KEY uk_progress_enrollment_lesson (enrollment_id, lesson_id),

    INDEX idx_progress_enrollment   (enrollment_id),

    CONSTRAINT fk_progress_enrollment
        FOREIGN KEY (enrollment_id) REFERENCES enrollments(id)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Tiến độ từng bài học. UNIQUE(enrollment_id, lesson_id) để UPSERT đúng từ Redis flush';


CREATE TABLE quiz_attempts (
    id              VARCHAR(36)     NOT NULL,
    enrollment_id   VARCHAR(36)     NOT NULL,
    lesson_id       VARCHAR(36)     NOT NULL    COMMENT 'ID của bài Quiz trong Course Service',
    score           INT             NOT NULL    DEFAULT 0 COMMENT 'Điểm số 0-100',
    is_passed       TINYINT(1)      NOT NULL    DEFAULT 0,
    submitted_at    DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP,

    created_at      DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by      VARCHAR(36)     NULL,
    updated_by      VARCHAR(36)     NULL,
    is_deleted      TINYINT(1)      NOT NULL    DEFAULT 0,
    version         BIGINT          NOT NULL    DEFAULT 0,

    PRIMARY KEY (id),

    -- Cho phép thi nhiều lần — không có UNIQUE, mỗi lần submit = 1 bản ghi mới
    INDEX idx_quiz_attempts_enrollment          (enrollment_id),
    INDEX idx_quiz_attempts_enrollment_lesson   (enrollment_id, lesson_id),

    CONSTRAINT fk_quiz_attempts_enrollment
        FOREIGN KEY (enrollment_id) REFERENCES enrollments(id)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Lịch sử làm bài quiz. Cho phép thi nhiều lần nên không có UNIQUE';


CREATE TABLE reviews (
    id              VARCHAR(36)     NOT NULL,
    enrollment_id   VARCHAR(36)     NOT NULL,

    -- Denormalized để query reviews theo courseId cực nhanh
    -- Không cần JOIN sang enrollments chỉ để lấy course_id
    course_id       VARCHAR(36)     NOT NULL    COMMENT 'Denormalized từ enrollment',

    star            TINYINT         NOT NULL    COMMENT '1-5 sao',
    comment         TEXT            NULL,

    created_at      DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by      VARCHAR(36)     NULL,
    updated_by      VARCHAR(36)     NULL,
    is_deleted      TINYINT(1)      NOT NULL    DEFAULT 0,
    version         BIGINT          NOT NULL    DEFAULT 0,

    PRIMARY KEY (id),

    -- 1 enrollment chỉ được review 1 lần — enforce ở DB
    UNIQUE KEY uk_review_enrollment (enrollment_id),

    INDEX idx_review_course     (course_id, created_at DESC),
    INDEX idx_review_star       (course_id, star),

    CONSTRAINT fk_review_enrollment
        FOREIGN KEY (enrollment_id) REFERENCES enrollments(id)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Đánh giá khóa học. UNIQUE(enrollment_id) đảm bảo 1 lượt ghi danh = 1 đánh giá';
