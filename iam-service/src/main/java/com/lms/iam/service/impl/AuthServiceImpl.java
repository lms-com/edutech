package com.lms.iam.service.impl;

import com.lms.common.exception.AppException;
import com.lms.iam.dto.request.LoginRequest;
import com.lms.iam.dto.request.LogoutRequest;
import com.lms.iam.dto.request.RegisterRequest;
import com.lms.iam.dto.response.LoginResponse;
import com.lms.iam.dto.response.RegisterResponse;
import com.lms.iam.exception.IamErrorCode;
import com.lms.iam.model.*;
import com.lms.iam.repository.LearnerProfileRepository;
import com.lms.iam.repository.UserRepository;
import com.lms.iam.repository.UserRoleRepository;
import com.lms.iam.security.CustomUserDetails;
import com.lms.iam.security.JwtService;
import com.lms.iam.service.AuthService;
import com.lms.iam.service.DeviceManagementService;
import com.lms.iam.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.iam.config.RabbitMQConfig;
import com.lms.iam.dto.event.SendOtpEvent;
import com.lms.iam.dto.request.ForgotPasswordRequest;
import com.lms.iam.dto.request.GoogleLoginRequest;
import com.lms.iam.dto.request.RegisterConfirmRequest;
import com.lms.iam.dto.request.RegisterInitRequest;
import com.lms.iam.dto.request.RegisterPendingData;
import com.lms.iam.dto.request.ResetPasswordRequest;
import com.lms.iam.dto.request.VerifyOtpRequest;
import com.lms.iam.security.CustomUserDetailsService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.security.SecureRandom;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final LearnerProfileRepository learnerProfileRepository;
    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final DeviceManagementService deviceManagementService;
    private final RoleService roleService;
    private final RabbitTemplate rabbitTemplate;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final CustomUserDetailsService userDetailsService;

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        // Kiem tra, xac thuc email/password o day sau do tra ve Authentication
        try {
            Authentication authentication = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
            );

            // Sau khi dang nhap thanh cong, tao JWT token cho client
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String userId = userDetails.getUser().getId();
                // Lay, kiem tra, va them deviceFingerPrint vao claim cua Token
            String devicefingerPrint = loginRequest.getDeviceFingerPrint();
            if (devicefingerPrint == null) {
                throw new AppException(IamErrorCode.DEVICE_FINGERPRINT_REQUIRED);
            } else if (deviceManagementService.existsInBlackList(userId, devicefingerPrint)) {
                throw new AppException(IamErrorCode.DEVICE_IS_BLOCKED);
            }
            String token = jwtService.generateToken(userDetails, devicefingerPrint);

            // Dang ki user:device vao redis
            deviceManagementService.registerDevice(userDetails.getUser().getId(), devicefingerPrint);

            // Cho JWT token vao response tra ve cho client
            return LoginResponse.builder()
                    .accessToken(token)
                    .userId(userDetails.getUser().getId())
                    .email(userDetails.getUsername())
                    .permissions(userDetails.getPermissions())
                    .build();
        }
        catch (LockedException e) {
            throw new AppException(IamErrorCode.USER_LOCKED, e.getMessage());
        }
        catch (DisabledException e) {
            throw new AppException(IamErrorCode.USER_DISABLED, e.getMessage());
        }
        catch (UsernameNotFoundException e) {
            throw new AppException(IamErrorCode.USER_NOT_EXISTED);
        }
        catch (BadCredentialsException e) {
            throw new AppException(IamErrorCode.PASSWORD_INCORRECT);
        }
    }

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest registerRequest) {
        // Kiem tra email da tung dang ki tai khoan chua
        Optional<User> user = userRepository.findByEmail(registerRequest.getEmail());
        if (user.isPresent()) {
            throw new AppException(IamErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // Luu thong tin nguoi dung
        String passwordHash = passwordEncoder.encode(registerRequest.getPassword());
        User newUser = User.builder()
                .email(registerRequest.getEmail())
                .password(passwordHash)
                .status(Userstatus.ACTIVE)
                .fullName(registerRequest.getFullName())
                .dob(registerRequest.getDob())
                .build();
        User savedUser = userRepository.save(newUser);

        // Mac dinh role "LEARNER" va luu vao UserRole
        try {
            Role role = roleService.getRoleDetails("LEARNER");

            // Tao UserRole de gan role cho user
            UserRole userRole = UserRole.builder()
                    .userId(newUser.getId())
                    .roleId(role.getId())
                    .build();
            log.info("👨‍🎓 A Learner register successfully! -> userId={}, roleId={}", userRole.getUserId(), userRole.getRoleId());
            userRoleRepository.save(userRole);
        } catch(AppException e) {
            throw new AppException(e.getErrorCode(), "Role Not Found: LEARNER");
        }

        // Tao va luu Learner Profile
        LearnerProfile profile = LearnerProfile.builder()
                .userId(newUser.getId())
                .build();
        learnerProfileRepository.save(profile);

        return RegisterResponse.builder()
                .email(savedUser.getEmail())
                .userId(savedUser.getId())
                .message("Register successfully")
                .build();
    }

    @Override
    public void logout(String userId, LogoutRequest request) {
        // Xoa user:device khoi redis
        deviceManagementService.deleteUserDevice(userId, request.getDeviceFingerPrint());
    }

    @Override
    public void sendForgotPasswordOtp(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(IamErrorCode.USER_NOT_EXISTED, "Email không tồn tại trong hệ thống."));

        // Sinh mã OTP 6 chữ số ngẫu nhiên an toàn
        String otpCode = String.format("%06d", new SecureRandom().nextInt(1_000_000));

        // Lưu mã OTP vào Redis với thời hạn sống 5 phút
        String redisKey = "auth:otp:" + email;
        redisTemplate.opsForValue().set(redisKey, otpCode, 5, TimeUnit.MINUTES);

        // Bắn sự kiện lên RabbitMQ để notification-service gửi email ngầm qua SMTP/SendGrid
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_IAM,
                    RabbitMQConfig.ROUTING_KEY_OTP,
                    SendOtpEvent.builder().email(email).otpCode(otpCode).build()
            );
            log.info("✔ [IAM] Đã phát sự kiện SendOtpEvent cho email [{}], mã OTP: [{}]", email, otpCode);
        } catch (Exception e) {
            log.error("❌ [IAM] Thất bại khi gửi sự kiện OTP qua RabbitMQ: {}", e.getMessage());
        }
    }

    @Override
    public void verifyOtp(VerifyOtpRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        String redisKey = "auth:otp:" + email;
        String savedOtp = redisTemplate.opsForValue().get(redisKey);

        if (savedOtp == null) {
            throw new AppException(IamErrorCode.OTP_EXPIRED, "Mã OTP đã hết hạn hoặc chưa được tạo. Vui lòng gửi lại yêu cầu.");
        }
        if (!savedOtp.equals(request.getOtp().trim())) {
            throw new AppException(IamErrorCode.OTP_INVALID, "Mã OTP không chính xác. Vui lòng kiểm tra lại.");
        }

        // Đánh dấu email này đã xác thực OTP thành công (thời hạn 10 phút)
        redisTemplate.opsForValue().set("auth:otp_verified:" + email, "true", 10, TimeUnit.MINUTES);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(IamErrorCode.USER_NOT_EXISTED, "Tài khoản không tồn tại."));

        String verifiedKey = "auth:otp_verified:" + email;
        String isVerified = redisTemplate.opsForValue().get(verifiedKey);
        String otpKey = "auth:otp:" + email;
        String savedOtp = redisTemplate.opsForValue().get(otpKey);

        boolean otpMatches = savedOtp != null && savedOtp.equals(request.getOtp().trim());
        boolean alreadyVerified = "true".equals(isVerified);

        if (!otpMatches && !alreadyVerified) {
            throw new AppException(IamErrorCode.OTP_INVALID, "Mã OTP không hợp lệ hoặc chưa được xác thực.");
        }

        // Cập nhật mật khẩu mới mã hóa BCrypt
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Thu hồi toàn bộ phiên đăng nhập cũ để bảo mật
        try {
            deviceManagementService.deleteAllDevicesOfUser(user.getId());
        } catch (Exception e) {
            log.warn("Không thể thu hồi các thiết bị cũ sau khi đổi mật khẩu: {}", e.getMessage());
        }

        // Xóa sạch các token OTP khỏi Redis
        redisTemplate.delete(otpKey);
        redisTemplate.delete(verifiedKey);
        log.info("✔ [IAM] Đã đặt lại mật khẩu thành công cho tài khoản [{}]", email);
    }

    @Override
    public void initiateRegister(RegisterInitRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.findByEmail(email).isPresent()) {
            throw new AppException(IamErrorCode.EMAIL_ALREADY_EXISTS, "Email này đã được sử dụng. Vui lòng đăng nhập hoặc chọn email khác.");
        }

        // Sinh mã OTP 6 số ngẫu nhiên
        String otpCode = String.format("%06d", new SecureRandom().nextInt(1_000_000));

        RegisterPendingData pendingData = RegisterPendingData.builder()
                .email(email)
                .fullName(request.getFullName().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .otpCode(otpCode)
                .build();

        try {
            String json = objectMapper.writeValueAsString(pendingData);
            redisTemplate.opsForValue().set("auth:register_pending:" + email, json, 5, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("Lỗi khi lưu thông tin đăng ký tạm vào Redis: {}", e.getMessage());
            throw new AppException(IamErrorCode.UNAUTHENTICATED, "Không thể khởi tạo phiên đăng ký. Vui lòng thử lại.");
        }

        // Gửi sự kiện SendOtpEvent qua RabbitMQ
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_IAM,
                    RabbitMQConfig.ROUTING_KEY_OTP,
                    SendOtpEvent.builder().email(email).otpCode(otpCode).build()
            );
            log.info("✔ [IAM] Đã gửi mã OTP đăng ký cho email [{}]", email);
        } catch (Exception e) {
            log.error("❌ [IAM] Thất bại khi gửi sự kiện OTP đăng ký qua RabbitMQ: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public LoginResponse confirmRegister(RegisterConfirmRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        String pendingKey = "auth:register_pending:" + email;
        String json = redisTemplate.opsForValue().get(pendingKey);

        if (json == null) {
            throw new AppException(IamErrorCode.OTP_EXPIRED, "Mã OTP đã hết hạn hoặc phiên đăng ký không tồn tại. Vui lòng đăng ký lại.");
        }

        RegisterPendingData pendingData;
        try {
            pendingData = objectMapper.readValue(json, RegisterPendingData.class);
        } catch (Exception e) {
            throw new AppException(IamErrorCode.OTP_INVALID, "Dữ liệu đăng ký không hợp lệ.");
        }

        if (!pendingData.getOtpCode().equals(request.getOtp().trim())) {
            throw new AppException(IamErrorCode.OTP_INVALID, "Mã OTP không chính xác. Vui lòng kiểm tra lại hộp thư.");
        }

        // Kiểm tra lại nếu tài khoản đã tồn tại
        if (userRepository.findByEmail(email).isPresent()) {
            redisTemplate.delete(pendingKey);
            throw new AppException(IamErrorCode.EMAIL_ALREADY_EXISTS, "Email này đã được sử dụng.");
        }

        // Tạo User chính thức
        User newUser = User.builder()
                .email(email)
                .password(pendingData.getPasswordHash())
                .fullName(pendingData.getFullName())
                .status(Userstatus.ACTIVE)
                .build();
        User savedUser = userRepository.save(newUser);

        // Gán role mặc định LEARNER
        try {
            Role role = roleService.getRoleDetails("LEARNER");
            UserRole userRole = UserRole.builder()
                    .userId(savedUser.getId())
                    .roleId(role.getId())
                    .build();
            userRoleRepository.save(userRole);
        } catch (Exception e) {
            log.warn("Không tìm thấy role LEARNER khi đăng ký: {}", e.getMessage());
        }

        // Tạo hồ sơ LearnerProfile
        LearnerProfile profile = LearnerProfile.builder()
                .userId(savedUser.getId())
                .build();
        learnerProfileRepository.save(profile);

        // Xóa dữ liệu tạm trong Redis
        redisTemplate.delete(pendingKey);
        log.info("✔ [IAM] Xác thực email thành công! Đã tạo tài khoản chính thức: [{}]", email);

        // Tự động đăng nhập nếu có deviceFingerPrint
        String deviceFingerPrint = request.getDeviceFingerPrint();
        if (deviceFingerPrint != null && !deviceFingerPrint.isBlank()) {
            if (!deviceManagementService.existsInBlackList(savedUser.getId(), deviceFingerPrint)) {
                CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(email);
                String token = jwtService.generateToken(userDetails, deviceFingerPrint);
                deviceManagementService.registerDevice(savedUser.getId(), deviceFingerPrint);
                return LoginResponse.builder()
                        .accessToken(token)
                        .userId(savedUser.getId())
                        .email(savedUser.getEmail())
                        .permissions(userDetails.getPermissions())
                        .build();
            }
        }

        return LoginResponse.builder()
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .build();
    }

    @Override
    @Transactional
    public LoginResponse loginWithGoogle(GoogleLoginRequest request) {
        String idToken = request.getIdToken().trim();
        String googleEmail;
        String googleName;

        if (idToken.startsWith("mock_google_")) {
            // Mock token cho môi trường test hoặc local
            googleEmail = idToken.substring("mock_google_".length()).trim().toLowerCase();
            googleName = "Google User (" + googleEmail.split("@")[0] + ")";
        } else {
            // Xác thực thật với Google OAuth2 API
            try {
                String verifyUrl = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;
                ResponseEntity<Map> response = restTemplate.getForEntity(verifyUrl, Map.class);
                Map body = response.getBody();
                if (body == null || !response.getStatusCode().is2xxSuccessful()) {
                    throw new AppException(IamErrorCode.INVALID_GOOGLE_TOKEN);
                }

                googleEmail = (String) body.get("email");
                googleName = (String) body.get("name");
                String emailVerified = String.valueOf(body.get("email_verified"));

                if (googleEmail == null || !"true".equalsIgnoreCase(emailVerified)) {
                    throw new AppException(IamErrorCode.INVALID_GOOGLE_TOKEN, "Tài khoản Google chưa được xác minh email.");
                }
            } catch (RestClientException e) {
                log.error("Lỗi khi xác thực Google token: {}", e.getMessage());
                throw new AppException(IamErrorCode.INVALID_GOOGLE_TOKEN, "Google token không hợp lệ hoặc đã hết hạn.");
            }
        }

        googleEmail = googleEmail.trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmail(googleEmail);
        User user;

        if (userOpt.isEmpty()) {
            // Tự động đăng ký người dùng mới từ Google
            User newUser = User.builder()
                    .email(googleEmail)
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .fullName(googleName != null && !googleName.isBlank() ? googleName : googleEmail)
                    .status(Userstatus.ACTIVE)
                    .build();
            user = userRepository.save(newUser);

            try {
                Role role = roleService.getRoleDetails("LEARNER");
                UserRole userRole = UserRole.builder()
                        .userId(user.getId())
                        .roleId(role.getId())
                        .build();
                userRoleRepository.save(userRole);
            } catch (Exception e) {
                log.warn("Không tìm thấy role LEARNER khi tạo tài khoản Google: {}", e.getMessage());
            }

            LearnerProfile profile = LearnerProfile.builder()
                    .userId(user.getId())
                    .build();
            learnerProfileRepository.save(profile);
            log.info("✔ [IAM] Đã tự động tạo tài khoản Google mới: [{}]", googleEmail);
        } else {
            user = userOpt.get();
            if (user.getStatus() == Userstatus.BANNED) {
                throw new AppException(IamErrorCode.USER_LOCKED, "Tài khoản của bạn đã bị khóa.");
            }
            if (user.getStatus() == Userstatus.INACTIVE || user.getStatus() == Userstatus.DELETED) {
                throw new AppException(IamErrorCode.USER_DISABLED, "Tài khoản của bạn đã bị vô hiệu hóa.");
            }
        }

        String deviceFingerprint = request.getDeviceFingerPrint();
        if (deviceFingerprint == null || deviceFingerprint.isBlank()) {
            throw new AppException(IamErrorCode.DEVICE_FINGERPRINT_REQUIRED);
        }

        if (deviceManagementService.existsInBlackList(user.getId(), deviceFingerprint)) {
            throw new AppException(IamErrorCode.DEVICE_IS_BLOCKED);
        }

        CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(googleEmail);
        String token = jwtService.generateToken(userDetails, deviceFingerprint);
        deviceManagementService.registerDevice(user.getId(), deviceFingerprint);

        return LoginResponse.builder()
                .accessToken(token)
                .userId(user.getId())
                .email(user.getEmail())
                .permissions(userDetails.getPermissions())
                .build();
    }
}
