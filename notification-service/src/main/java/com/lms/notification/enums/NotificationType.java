package com.lms.notification.enums;

public enum NotificationType {
    ORDER_COMPLETED,    // Mua khóa học thành công
    COURSE_APPROVED,    // Admin duyệt khóa học
    COURSE_REJECTED,    // Admin từ chối khóa học
    COURSE_COMPLETED,   // Học viên hoàn thành 100%
    OTP_SENT            // Gửi OTP quên mật khẩu
}