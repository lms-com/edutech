package com.lms.notification.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lms.notification.entity.Certificate;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, String> {

    // Tìm chứng chỉ của một học viên
    List<Certificate> findByLearnerId(String learnerId);

    // Chống sinh trùng certificate cho cùng 1 enrollment
    boolean existsByEnrollmentId(String enrollmentId);

    // Kiểm tra chứng chỉ hợp lệ bằng mã QR Hash (Dùng cho nhà tuyển dụng kiểm tra)
    Optional<Certificate> findByQrCodeHash(String qrCodeHash);
}
