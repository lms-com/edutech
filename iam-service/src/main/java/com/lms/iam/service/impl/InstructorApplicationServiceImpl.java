package com.lms.iam.service.impl;

import com.lms.common.exception.AppException;
import com.lms.iam.config.RabbitMQConfig;
import com.lms.iam.dto.event.InstructorApprovedEvent;
import com.lms.iam.dto.request.ReviewInstructorApplicationRequest;
import com.lms.iam.dto.request.SubmitInstructorApplicationRequest;
import com.lms.iam.dto.response.InstructorApplicationResponse;
import com.lms.iam.exception.IamErrorCode;
import com.lms.iam.model.*;
import com.lms.iam.repository.*;
import com.lms.iam.service.InstructorApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InstructorApplicationServiceImpl implements InstructorApplicationService {

    private final InstructorApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final InstructorProfileRepository instructorProfileRepository;
    private final RabbitTemplate rabbitTemplate;

    @Override
    @Transactional
    public InstructorApplicationResponse submitApplication(String userId, SubmitInstructorApplicationRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(IamErrorCode.USER_NOT_EXISTED, "Không tìm thấy người dùng."));

        // Kiểm tra xem người dùng đã là INSTRUCTOR chưa
        boolean alreadyInstructor = userRoleRepository.existsByUserIdAndRoleName(userId, "INSTRUCTOR");
        if (alreadyInstructor) {
            throw new AppException(IamErrorCode.UNAUTHENTICATED, "Bạn đã có vai trò Giảng viên (INSTRUCTOR) trên hệ thống.");
        }

        // Kiểm tra xem có đơn PENDING nào chưa
        applicationRepository.findFirstByUserIdAndStatus(userId, ApplicationStatus.PENDING)
                .ifPresent(app -> {
                    throw new AppException(IamErrorCode.UNAUTHENTICATED, "Bạn đã có đơn đăng ký đang chờ phê duyệt. Vui lòng đợi quản trị viên xử lý.");
                });

        InstructorApplication application = InstructorApplication.builder()
                .userId(userId)
                .bio(request.getBio().trim())
                .expertise(request.getExpertise().trim())
                .experienceYears(request.getExperienceYears() != null ? request.getExperienceYears() : 0)
                .portfolioUrl(request.getPortfolioUrl() != null ? request.getPortfolioUrl().trim() : null)
                .cvUrl(request.getCvUrl() != null ? request.getCvUrl().trim() : null)
                .status(ApplicationStatus.PENDING)
                .build();

        application = applicationRepository.save(application);
        log.info("✔ [IAM] Người dùng [{}] đã nộp đơn đăng ký giảng viên thành công. ID đơn: [{}]", user.getEmail(), application.getId());

        return mapToResponse(application, user);
    }

    @Override
    public List<InstructorApplicationResponse> getMyApplications(String userId) {
        User user = userRepository.findById(userId).orElse(null);
        return applicationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(app -> mapToResponse(app, user))
                .toList();
    }

    @Override
    public Page<InstructorApplicationResponse> getApplicationsForAdmin(ApplicationStatus status, Pageable pageable) {
        Page<InstructorApplication> page;
        if (status != null) {
            page = applicationRepository.findByStatus(status, pageable);
        } else {
            page = applicationRepository.findAll(pageable);
        }

        return page.map(app -> {
            User user = userRepository.findById(app.getUserId()).orElse(null);
            return mapToResponse(app, user);
        });
    }

    @Override
    @Transactional
    public InstructorApplicationResponse reviewApplication(String adminId, String applicationId, ReviewInstructorApplicationRequest request) {
        InstructorApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new AppException(IamErrorCode.USER_NOT_EXISTED, "Không tìm thấy đơn đăng ký giảng viên."));

        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new AppException(IamErrorCode.UNAUTHENTICATED, "Đơn đăng ký này đã được xử lý trước đó (" + application.getStatus() + ").");
        }

        User user = userRepository.findById(application.getUserId())
                .orElseThrow(() -> new AppException(IamErrorCode.USER_NOT_EXISTED, "Người dùng nộp đơn không tồn tại."));

        boolean approved = Boolean.TRUE.equals(request.getApproved());
        application.setReviewedBy(adminId);
        application.setReviewedAt(Instant.now());

        if (approved) {
            application.setStatus(ApplicationStatus.APPROVED);
            application.setRejectionReason(null);

            // 1. Gán role INSTRUCTOR vào user_roles nếu chưa có
            boolean hasInstructorRole = userRoleRepository.existsByUserIdAndRoleName(user.getId(), "INSTRUCTOR");
            if (!hasInstructorRole) {
                Role instructorRole = roleRepository.findRoleByRoleName("INSTRUCTOR")
                        .orElseThrow(() -> new AppException(IamErrorCode.ROLE_NOT_FOUND, "Vai trò INSTRUCTOR chưa được cấu hình."));
                UserRole userRole = UserRole.builder()
                        .userId(user.getId())
                        .roleId(instructorRole.getId())
                        .build();
                userRoleRepository.save(userRole);
            }

            // 2. Tạo hoặc cập nhật instructor_profiles
            BigDecimal commissionRate = request.getDefaultCommissionRate() != null
                    ? request.getDefaultCommissionRate()
                    : new BigDecimal("0.70");

            InstructorProfile profile = instructorProfileRepository.findById(user.getId())
                    .orElse(InstructorProfile.builder()
                            .userId(user.getId())
                            .bio(application.getBio())
                            .defaultCommissionRate(commissionRate)
                            .build());
            profile.setBio(application.getBio());
            profile.setDefaultCommissionRate(commissionRate);
            instructorProfileRepository.save(profile);

            // 3. Bắn event InstructorApprovedEvent qua RabbitMQ
            try {
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.EXCHANGE_IAM,
                        RabbitMQConfig.ROUTING_KEY_INSTRUCTOR_APPROVED,
                        InstructorApprovedEvent.builder()
                                .userId(user.getId())
                                .userEmail(user.getEmail())
                                .fullName(user.getFullName())
                                .commissionRate(commissionRate)
                                .approvedAt(Instant.now())
                                .build()
                );
                log.info("✔ [IAM] Đã bắn InstructorApprovedEvent cho giảng viên [{}] ({})", user.getFullName(), user.getEmail());
            } catch (Exception e) {
                log.error("❌ [IAM] Lỗi bắn sự kiện duyệt giảng viên qua RabbitMQ: {}", e.getMessage());
            }
        } else {
            application.setStatus(ApplicationStatus.REJECTED);
            application.setRejectionReason(request.getRejectionReason() != null ? request.getRejectionReason().trim() : "Hồ sơ chưa đạt yêu cầu chuyên môn.");
            log.info("ℹ [IAM] Đã từ chối đơn đăng ký giảng viên của [{}]. Lý do: {}", user.getEmail(), application.getRejectionReason());
        }

        application = applicationRepository.save(application);
        return mapToResponse(application, user);
    }

    private InstructorApplicationResponse mapToResponse(InstructorApplication app, User user) {
        return InstructorApplicationResponse.builder()
                .id(app.getId())
                .userId(app.getUserId())
                .applicantName(user != null ? user.getFullName() : "N/A")
                .applicantEmail(user != null ? user.getEmail() : "N/A")
                .bio(app.getBio())
                .expertise(app.getExpertise())
                .experienceYears(app.getExperienceYears())
                .portfolioUrl(app.getPortfolioUrl())
                .cvUrl(app.getCvUrl())
                .status(app.getStatus())
                .rejectionReason(app.getRejectionReason())
                .reviewedBy(app.getReviewedBy())
                .reviewedAt(app.getReviewedAt())
                .createdAt(app.getCreatedAt())
                .updatedAt(app.getUpdatedAt())
                .build();
    }
}
