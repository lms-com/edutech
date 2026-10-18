-- V3__add_rejection_note_to_courses.sql
-- Thêm cột rejection_note để lưu lý do Admin từ chối khóa học
ALTER TABLE courses ADD COLUMN rejection_note TEXT DEFAULT NULL AFTER status;
