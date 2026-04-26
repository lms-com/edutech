package com.lms.iam.service;

import com.lms.iam.dto.response.UserDeviceResponse;

import java.util.List;

public interface DeviceManagementService {
    int MAX_ALLOWED_DEVICES = 2;
    int DEVICE_EXPIRATION_DAYS = 30;

    void registerDevice (String userId, String deviceFingerPrint);

    List<UserDeviceResponse> getUserDevices (String userId);

    String getUserDeviceRedisKey (String userId);

    String getUserBlackListRedisKey (String userId);

    void deleteUserDevice (String userId, String deviceFingerPrint);

    boolean existsInBlackList (String userId, String deviceFingerPrint);

    void addToBlackList (String userId, String deviceFingerPrint);
}