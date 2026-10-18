-- Tạo mã giảm giá SUMMER2026 giảm 10%, hạn dùng trong năm 2026, giới hạn 100 lượt
INSERT INTO promotions (id, code, discount_percent, discount_amount, currency_code, start_date, end_date, usage_limit, usage_count, is_active, version)
VALUES ('promo-test-01', 'SUMMER2026', 10.00, NULL, NULL, '2026-01-01 00:00:00', '2026-12-31 23:59:59', 100, 0, TRUE, 0);

-- Gắn mã này cho một khóa học giả định có ID là 'course-test-123'
INSERT INTO course_promotions (id, promotion_id, course_id)
VALUES ('cp-test-01', 'promo-test-01', 'course-test-123');