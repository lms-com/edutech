package com.lms.iam.service;

import com.lms.iam.dto.request.ForgotPasswordRequest;
import com.lms.iam.dto.request.LoginRequest;
import com.lms.iam.dto.request.LogoutRequest;
import com.lms.iam.dto.request.RegisterRequest;
import com.lms.iam.dto.request.ResetPasswordRequest;
import com.lms.iam.dto.request.VerifyOtpRequest;
import com.lms.iam.dto.response.LoginResponse;
import com.lms.iam.dto.response.RegisterResponse;

public interface AuthService {

    LoginResponse login(LoginRequest loginRequest);

    RegisterResponse register(RegisterRequest registerRequest);

    void logout(String userId, LogoutRequest request);

    void sendForgotPasswordOtp(ForgotPasswordRequest request);

    void verifyOtp(VerifyOtpRequest request);

    void resetPassword(ResetPasswordRequest request);
}
