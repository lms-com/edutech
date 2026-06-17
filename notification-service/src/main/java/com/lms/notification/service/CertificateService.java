package com.lms.notification.service;

import com.lms.notification.entity.Certificate;
import java.util.List;

public interface CertificateService {
    void generateCertificate(String learnerId, String courseId, String enrollmentId);

    List<Certificate> getMyCertificates(String learnerId);

    Certificate verifyCertificate(String qrCodeHash);
}
