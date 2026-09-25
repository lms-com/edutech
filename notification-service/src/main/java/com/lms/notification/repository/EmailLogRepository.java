package com.lms.notification.repository;

import com.lms.notification.entity.EmailLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, String> {
    // Hiện tại chỉ cần các hàm CRUD cơ bản của JpaRepository là đủ dùng
}