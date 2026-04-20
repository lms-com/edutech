-- V1: Init Course Schema

CREATE TABLE categories (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    parent_id VARCHAR(36),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    updated_by VARCHAR(36),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE courses (
    id VARCHAR(36) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    summary TEXT,
    description TEXT,
    thumbnail_url VARCHAR(500),
    video_url VARCHAR(500),
    price DECIMAL(15, 2) NOT NULL DEFAULT 0.0,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    level VARCHAR(20) NOT NULL,
    instructor_id VARCHAR(36) NOT NULL,
    category_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    updated_by VARCHAR(36),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_course_category FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE TABLE sections (
    id VARCHAR(36) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    course_id VARCHAR(36) NOT NULL,
    order_index INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    updated_by VARCHAR(36),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_section_course FOREIGN KEY (course_id) REFERENCES courses(id)
);

CREATE TABLE lessons (
    id VARCHAR(36) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    section_id VARCHAR(36) NOT NULL,
    content TEXT,
    video_url VARCHAR(500),
    duration INT NOT NULL DEFAULT 0, -- Duration in seconds
    is_free BOOLEAN NOT NULL DEFAULT FALSE,
    order_index INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    updated_by VARCHAR(36),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_lesson_section FOREIGN KEY (section_id) REFERENCES sections(id)
);
