CREATE TABLE daily_course_statistics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id VARCHAR(255) NOT NULL,              -- Mã khóa học
    instructor_id VARCHAR(255) NOT NULL,          -- Mã giảng viên (để nhóm dữ liệu cho Instructor)
    record_date DATE NOT NULL,              -- Ngày ghi nhận (VD: 2026-10-10)

    total_order_amount DECIMAL(15, 2) DEFAULT 0, -- TỔNG TIỀN học viên trả (Cho Admin xem GMV)
    instructor_revenue DECIMAL(15, 2) DEFAULT 0, -- Tiền INSTRUCTOR nhận (Sau khi trừ phí)
    platform_fee DECIMAL(15, 2) DEFAULT 0,       -- Tiền SÀN nhận (Hoa hồng của Admin)

    daily_enrollments INT DEFAULT 0,        -- Số lượt mua TRONG NGÀY

    refunded_amount DECIMAL(15, 2) DEFAULT 0, -- Số tiền bị hoàn lại ghi nhận trong ngày
    refunded_enrollments INT DEFAULT 0,        -- Số lượt học viên hoàn khóa học trong ngày

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- Ràng buộc: Mỗi ngày, một khóa học chỉ có duy nhất 1 dòng
    UNIQUE KEY uk_course_date (course_id, record_date)
);

-- Tạo sẵn index để query biểu đồ cho nhanh
CREATE INDEX idx_instructor_date ON daily_course_statistics(instructor_id, record_date);
CREATE INDEX idx_record_date ON daily_course_statistics(record_date);