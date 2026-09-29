package com.lms.notification.service;

import com.lms.notification.dto.response.CertificateResponse;

import java.util.List;

public interface CertificateService {
    void generateCertificate(String learnerId, String courseId, String enrollmentId);

    List<CertificateResponse> getMyCertificates(String learnerId);

    CertificateResponse verifyCertificate(String qrCodeHash);
}
