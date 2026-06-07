package com.lms.notification.repository;

import com.lms.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {

    // Tìm kiếm thông báo theo từng User (Học viên/Giảng viên) phục vụ API GET /me (Có phân trang)
    Page<Notification> findByUserId(String userId, Pageable pageable);

    // Tìm kiếm kèm bộ lọc đã đọc/chưa đọc (Ví dụ: Frontend muốn lọc riêng các tin chưa đọc)
    Page<Notification> findByUserIdAndIsRead(String userId, Boolean isRead, Pageable pageable);

    // Đếm số lượng thông báo CHƯA ĐỌC để hiển thị số badge màu đỏ trên quả chuông
    long countByUserIdAndIsReadFalse(String userId);

    // Tìm toàn bộ danh sách thông báo chưa đọc của 1 user để phục vụ tính năng "Đánh dấu tất cả đã đọc"
    List<Notification> findByUserIdAndIsReadFalse(String userId);
}