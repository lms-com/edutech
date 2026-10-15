package com.lms.notification.enums;

public enum NotificationType {
    ORDER_COMPLETED,    // Mua khóa học thành công
    COURSE_APPROVED,    // Admin duyệt khóa học
    COURSE_REJECTED,    // Admin từ chối khóa học
    COURSE_COMPLETED,   // Học viên hoàn thành 100%
    OTP_SENT,           // Gửi OTP quên mật khẩu
    PAYOUT_APPROVED,    // Admin duyệt lệnh rút tiền
    PAYOUT_REJECTED,    // Admin từ chối lệnh rút tiền
    REFUND_APPROVED,    // Admin duyệt hoàn tiền đơn hàng
    REFUND_REJECTED     // Admin từ chối hoàn tiền đơn hàng
}