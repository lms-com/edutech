package com.lms.notification.controller;

import com.lms.common.dto.response.ApiResponse;
import com.lms.notification.entity.Certificate;
import com.lms.notification.service.CertificateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/certificates")
@RequiredArgsConstructor
@Tag(name = "Certificate Controller", description = "Quản lý và xác minh chứng chỉ học viên")
public class CertificateController {

    private final CertificateService certificateService;

    @Operation(summary = "Lấy danh sách chứng chỉ của tôi")
    @GetMapping("/me")
    public ApiResponse<List<Certificate>> getMyCertificates(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        if (userId == null) {
            throw new IllegalArgumentException("X-User-Id header is missing");
        }
        return ApiResponse.success(certificateService.getMyCertificates(userId));
    }

    @Operation(summary = "Xác minh tính hợp lệ của chứng chỉ thông qua mã QR Hash")
    @GetMapping("/verify/{hash}")
    public ApiResponse<Certificate> verifyCertificate(@PathVariable String hash) {
        return ApiResponse.success(certificateService.verifyCertificate(hash));
    }
}
