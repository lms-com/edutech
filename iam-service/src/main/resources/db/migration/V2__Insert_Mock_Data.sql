-- ==============================================================================
-- 1. INSERT ROLES (Vai trò)
-- ==============================================================================
INSERT INTO roles (id, role_name) VALUES
    ('role-001-admin', 'ADMIN'),
    ('role-002-instructor', 'INSTRUCTOR'),
    ('role-003-learner', 'LEARNER');

-- ==============================================================================
-- 2. INSERT PERMISSIONS (Quyền hạn)
-- ==============================================================================
-- Quyền của Admin
INSERT INTO permissions (id, permission_key, resource_group) VALUES
     ('perm-001', 'USER_MANAGE', 'USER'),
     ('perm-002', 'SYSTEM_SETTING', 'SYSTEM'),
     ('perm-003', 'COURSE_APPROVE', 'COURSE'),
     ('perm-004', 'PAYOUT_MANAGE', 'FINANCE');

-- Quyền của Instructor
INSERT INTO permissions (id, permission_key, resource_group) VALUES
     ('perm-005', 'COURSE_CREATE', 'COURSE'),
     ('perm-006', 'COURSE_UPDATE', 'COURSE'),
     ('perm-007', 'COURSE_VIEW_OWN', 'COURSE'),
     ('perm-008', 'FINANCE_VIEW_OWN', 'FINANCE');

-- Quyền của Learner
INSERT INTO permissions (id, permission_key, resource_group) VALUES
     ('perm-009', 'COURSE_LEARN', 'COURSE'),
     ('perm-010', 'ORDER_CREATE', 'ORDER'),
     ('perm-011', 'REVIEW_CREATE', 'REVIEW');

-- ==============================================================================
-- 3. MAPPING ROLE_PERMISSIONS (Gán quyền cho vai trò)
-- ==============================================================================
-- Admin có toàn quyền (demo vài quyền)
INSERT INTO role_permissions (id, role_id, permission_id) VALUES
    ('rp-001', 'role-001-admin', 'perm-001'),
    ('rp-002', 'role-001-admin', 'perm-002'),
    ('rp-003', 'role-001-admin', 'perm-003'),
    ('rp-004', 'role-001-admin', 'perm-004');

-- Instructor có quyền tạo/sửa khóa học
INSERT INTO role_permissions (id, role_id, permission_id) VALUES
    ('rp-005', 'role-002-instructor', 'perm-005'),
    ('rp-006', 'role-002-instructor', 'perm-006'),
    ('rp-007', 'role-002-instructor', 'perm-007'),
    ('rp-008', 'role-002-instructor', 'perm-008');

-- Learner có quyền học và mua
INSERT INTO role_permissions (id, role_id, permission_id) VALUES
    ('rp-009', 'role-003-learner', 'perm-009'),
    ('rp-010', 'role-003-learner', 'perm-010'),
    ('rp-011', 'role-003-learner', 'perm-011');

-- ==============================================================================
-- 4. INSERT USERS (Tạo tài khoản) - Pass: 123456
-- Mật khẩu Bcrypt của 123456 là: $2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1ipc9WeVFibcVSlO
-- ==============================================================================
INSERT INTO users (id, email, password_hash, full_name, status) VALUES
    ('user-admin-01', 'admin@lms.com', '$2a$10$hKDVYxLefVhv/Etu6GzS9O.Z2O8O2m7S7.GueuKEn9Z1.0sF1Oa2u', 'System Admin', 'ACTIVE'),
    ('user-inst-01', 'giangvien@lms.com', '$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1ipc9WeVFibcVSlO', 'Tran Giang Vien', 'ACTIVE'),
    ('user-learn-01', 'hocvien@lms.com', '$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1ipc9WeVFibcVSlO', 'Nguyen Hoc Vien', 'ACTIVE');

-- ==============================================================================
-- 5. MAPPING USER_ROLES (Gán vai trò cho User)
-- ==============================================================================
INSERT INTO user_roles (id, user_id, role_id) VALUES
    ('ur-001', 'user-admin-01', 'role-001-admin'),
    ('ur-002', 'user-inst-01', 'role-002-instructor'),
    ('ur-003', 'user-learn-01', 'role-003-learner');

-- ==============================================================================
-- 6. INSERT PROFILES (Hồ sơ)
-- ==============================================================================
INSERT INTO instructor_profiles (user_id, bio, default_commission_rate) VALUES
    ('user-inst-01', 'Chuyên gia lập trình Java với 10 năm kinh nghiệm.', 0.70);

INSERT INTO learner_profiles (user_id, job) VALUES
    ('user-learn-01', 'Sinh viên IT');