package com.lms.iam.service;

import com.lms.iam.dto.request.ChangePasswordWithOtpRequest;
import com.lms.iam.dto.request.ChangePinRequest;
import com.lms.iam.dto.request.ResetPinRequest;
import com.lms.iam.dto.request.SetupPinRequest;
import com.lms.iam.dto.request.VerifyPinRequest;
import com.lms.iam.dto.response.SecurityStatusResponse;

public interface UserSecurityService {

    SecurityStatusResponse getSecurityStatus(String userId);

    void setupPin(String userId, SetupPinRequest request);

    void changePin(String userId, ChangePinRequest request);

    void verifyPin(String userId, VerifyPinRequest request);

    void sendResetPinOtp(String userId);

    void resetPin(String userId, ResetPinRequest request);

    void lockFinancialSession(String userId);

    void sendChangePasswordOtp(String userId);

    void changePasswordWithOtp(String userId, ChangePasswordWithOtpRequest request);
}
