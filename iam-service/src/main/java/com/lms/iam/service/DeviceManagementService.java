package com.lms.iam.service;

public interface DeviceManagementService {
    int MAX_ALLOWED_DEVICES = 2;
    int DEVICE_EXPIRATION_DAYS = 30;

    void registerDevice (String userId, String deviceFingerPrint);
}