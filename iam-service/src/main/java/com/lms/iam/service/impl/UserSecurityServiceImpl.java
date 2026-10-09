package com.lms.iam.service.impl;

import com.lms.common.exception.AppException;
import com.lms.iam.config.RabbitMQConfig;
import com.lms.iam.dto.event.SendOtpEvent;
import com.lms.iam.dto.request.ChangePasswordWithOtpRequest;
import com.lms.iam.dto.request.ChangePinRequest;
import com.lms.iam.dto.request.ResetPinRequest;
import com.lms.iam.dto.request.SetupPinRequest;
import com.lms.iam.dto.request.VerifyPinRequest;
import com.lms.iam.dto.response.SecurityStatusResponse;
import com.lms.iam.exception.IamErrorCode;
import com.lms.iam.model.User;
import com.lms.iam.repository.UserRepository;
import com.lms.iam.service.DeviceManagementService;
import com.lms.iam.service.UserSecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSecurityServiceImpl implements UserSecurityService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final DeviceManagementService deviceManagementService;

    private static final String FINANCIAL_SESSION_PREFIX = "auth:financial_session:";
    private static final String RESET_PIN_OTP_PREFIX = "auth:reset_pin_otp:";
    private static final String CHANGE_PWD_OTP_PREFIX = "auth:change_pwd_otp:";
    private static final int FINANCIAL_SESSION_MINUTES = 15;

    @Override
    public SecurityStatusResponse getSecurityStatus(String userId) {
        User user = getUser(userId);
        boolean hasPin = user.getPinHash() != null && !user.getPinHash().isBlank();

        String sessionKey = FINANCIAL_SESSION_PREFIX + userId;
        Long expire = redisTemplate.getExpire(sessionKey, TimeUnit.SECONDS);
        boolean isActive = expire != null && expire > 0;
        long remaining = isActive ? expire : 0;

        return SecurityStatusResponse.builder()
                .hasPin(hasPin)
                .financialSessionActive(isActive)
                .remainingSeconds(remaining)
                .build();
    }

    @Override
    @Transactional
    public void setupPin(String userId, SetupPinRequest request) {
        User user = getUser(userId);
        if (user.getPinHash() != null && !user.getPinHash().isBlank()) {
            throw new AppException(IamErrorCode.PIN_ALREADY_SET);
        }

        user.setPinHash(passwordEncoder.encode(request.getPin().trim()));
        userRepository.save(user);

        // Kích hoạt ngay phiên tài chính 15 phút sau khi cài đặt thành công
        redisTemplate.opsForValue().set(FINANCIAL_SESSION_PREFIX + userId, "true", FINANCIAL_SESSION_MINUTES, TimeUnit.MINUTES);
        log.info("✔ [IAM] Người dùng [{}] đã thiết lập mã PIN bảo mật cấp 2 thành công", userId);
    }

    @Override
    @Transactional
    public void changePin(String userId, ChangePinRequest request) {
        User user = getUser(userId);
        if (user.getPinHash() == null || user.getPinHash().isBlank()) {
            throw new AppException(IamErrorCode.PIN_NOT_SET);
        }

        if (!passwordEncoder.matches(request.getCurrentPin().trim(), user.getPinHash())) {
            throw new AppException(IamErrorCode.PIN_INCORRECT, "Mã PIN hiện tại không chính xác.");
        }

        user.setPinHash(passwordEncoder.encode(request.getNewPin().trim()));
        userRepository.save(user);

        // Gia hạn phiên tài chính
        redisTemplate.opsForValue().set(FINANCIAL_SESSION_PREFIX + userId, "true", FINANCIAL_SESSION_MINUTES, TimeUnit.MINUTES);
        log.info("✔ [IAM] Người dùng [{}] đã đổi mã PIN bảo mật cấp 2 thành công", userId);
    }

    @Override
    public void verifyPin(String userId, VerifyPinRequest request) {
        User user = getUser(userId);
        if (user.getPinHash() == null || user.getPinHash().isBlank()) {
            throw new AppException(IamErrorCode.PIN_NOT_SET);
        }

        if (!passwordEncoder.matches(request.getPin().trim(), user.getPinHash())) {
            throw new AppException(IamErrorCode.PIN_INCORRECT);
        }

        // Mở khóa phiên tài chính 15 phút trong Redis
        redisTemplate.opsForValue().set(FINANCIAL_SESSION_PREFIX + userId, "true", FINANCIAL_SESSION_MINUTES, TimeUnit.MINUTES);
        log.info("✔ [IAM] Người dùng [{}] đã mở khóa phiên tài chính thành công (15 phút)", userId);
    }

    @Override
    public void sendResetPinOtp(String userId) {
        User user = getUser(userId);
        String otpCode = generateOtp();
        redisTemplate.opsForValue().set(RESET_PIN_OTP_PREFIX + userId, otpCode, 5, TimeUnit.MINUTES);

        sendOtpViaRabbitMQ(user.getEmail(), otpCode, "khôi phục mã PIN bảo mật cấp 2");
    }

    @Override
    @Transactional
    public void resetPin(String userId, ResetPinRequest request) {
        String savedOtp = redisTemplate.opsForValue().get(RESET_PIN_OTP_PREFIX + userId);
        if (savedOtp == null) {
            throw new AppException(IamErrorCode.OTP_EXPIRED, "Mã OTP đã hết hạn hoặc chưa được yêu cầu.");
        }
        if (!savedOtp.equals(request.getOtp().trim())) {
            throw new AppException(IamErrorCode.OTP_INVALID);
        }

        User user = getUser(userId);
        user.setPinHash(passwordEncoder.encode(request.getNewPin().trim()));
        userRepository.save(user);

        redisTemplate.delete(RESET_PIN_OTP_PREFIX + userId);
        redisTemplate.opsForValue().set(FINANCIAL_SESSION_PREFIX + userId, "true", FINANCIAL_SESSION_MINUTES, TimeUnit.MINUTES);
        log.info("✔ [IAM] Người dùng [{}] đã đặt lại mã PIN cấp 2 bằng OTP email thành công", userId);
    }

    @Override
    public void lockFinancialSession(String userId) {
        redisTemplate.delete(FINANCIAL_SESSION_PREFIX + userId);
        log.info("🔒 [IAM] Đã khóa phiên tài chính của người dùng [{}]", userId);
    }

    @Override
    public void sendChangePasswordOtp(String userId) {
        User user = getUser(userId);
        String otpCode = generateOtp();
        redisTemplate.opsForValue().set(CHANGE_PWD_OTP_PREFIX + userId, otpCode, 5, TimeUnit.MINUTES);

        sendOtpViaRabbitMQ(user.getEmail(), otpCode, "đổi mật khẩu tài khoản");
    }

    @Override
    @Transactional
    public void changePasswordWithOtp(String userId, ChangePasswordWithOtpRequest request) {
        String savedOtp = redisTemplate.opsForValue().get(CHANGE_PWD_OTP_PREFIX + userId);
        if (savedOtp == null) {
            throw new AppException(IamErrorCode.OTP_EXPIRED, "Mã OTP đã hết hạn hoặc chưa được yêu cầu.");
        }
        if (!savedOtp.equals(request.getOtp().trim())) {
            throw new AppException(IamErrorCode.OTP_INVALID);
        }

        User user = getUser(userId);
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        redisTemplate.delete(CHANGE_PWD_OTP_PREFIX + userId);

        // Thu hồi toàn bộ phiên đăng nhập cũ
        try {
            deviceManagementService.deleteAllDevicesOfUser(userId);
        } catch (Exception e) {
            log.warn("Lỗi khi thu hồi phiên thiết bị cũ: {}", e.getMessage());
        }
        log.info("✔ [IAM] Người dùng [{}] đã đổi mật khẩu qua OTP email thành công", userId);
    }

    private User getUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(IamErrorCode.USER_NOT_EXISTED));
    }

    private String generateOtp() {
        return String.format("%06d", new SecureRandom().nextInt(1_000_000));
    }

    private void sendOtpViaRabbitMQ(String email, String otpCode, String purpose) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_IAM,
                    RabbitMQConfig.ROUTING_KEY_OTP,
                    SendOtpEvent.builder().email(email).otpCode(otpCode).build()
            );
            log.info("✔ [IAM] Đã gửi sự kiện SendOtpEvent ({}) cho email [{}]", purpose, email);
        } catch (Exception e) {
            log.error("❌ [IAM] Lỗi khi gửi OTP qua RabbitMQ: {}", e.getMessage());
        }
    }
}
