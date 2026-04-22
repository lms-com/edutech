package com.lms.iam.service.impl;

import com.lms.common.exception.AppException;
import com.lms.iam.dto.request.LoginRequest;
import com.lms.iam.dto.request.RegisterRequest;
import com.lms.iam.dto.response.LoginResponse;
import com.lms.iam.dto.response.RegisterResponse;
import com.lms.iam.exception.IamErrorCode;
import com.lms.iam.model.User;
import com.lms.iam.model.Userstatus;
import com.lms.iam.repository.UserRepository;
import com.lms.iam.security.CustomUserDetails;
import com.lms.iam.security.JwtService;
import com.lms.iam.service.AuthService;
import com.lms.iam.service.DeviceManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final DeviceManagementService deviceManagementService;

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        // Thuc hien kiem tra, xac thuc email/password o day sau do tra ve Authentication
        try {
            Authentication authentication = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
            );

            // Sau khi dang nhap thanh cong, tao JWT token cho client
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
                // Lay, kiem tra, va them deviceFingerPrint vao claim cua Token
            String devicefingerPrint = loginRequest.getDeviceFingerPrint();
            if (devicefingerPrint == null) {
                throw new AppException(IamErrorCode.DEVICE_FINGERPRINT_REQUIRED);
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
        } catch (UsernameNotFoundException e) {
            throw new AppException(IamErrorCode.USER_NOT_EXISTED);
        }
        catch (BadCredentialsException e) {
            throw new AppException(IamErrorCode.PASSWORD_INCORRECT);
        }
    }

    @Override
    public RegisterResponse register(RegisterRequest registerRequest) {
        Optional<User> user = userRepository.findByEmail(registerRequest.getEmail());
        if (user.isPresent()) {
            throw new AppException(IamErrorCode.EMAIL_ALREADY_EXISTS);
        }

        String passwordHash = passwordEncoder.encode(registerRequest.getPassword());
        User newUser = User.builder()
                .email(registerRequest.getEmail())
                .password(passwordHash)
                .status(Userstatus.ACTIVE)
                .fullName(registerRequest.getFullName())
                .dob(registerRequest.getDob())
                .build();
        User savedUser = userRepository.save(newUser);
        return RegisterResponse.builder()
                .email(savedUser.getEmail())
                .userId(savedUser.getId())
                .build();
    }
}
