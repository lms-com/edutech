package com.lms.iam.service.impl;

import com.lms.iam.service.DeviceManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceManagementServiceImpl implements DeviceManagementService {
    private final StringRedisTemplate redisTemplate;

    @Override
    public void registerDevice(String userId, String deviceFingerPrint) {
        // Tao key theo userId trong redis cho cap user:device
        String redisKey = "user:" + userId + ":device";
        // Tao ZSetOperations de luu cap user:device theo cau truc ZSet
        ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();

        // Dung thoi gian luu vao redis lam score, score thap nhat se bi xoa truoc
        long currentTime = System.currentTimeMillis();
            // Neu user:device da ton tai, thi chi cap nhat currentTime
        zSetOps.add(redisKey, deviceFingerPrint, currentTime);

        // Kiem so thiet bi da dang ki cua user
        Long currentDeviceCount = zSetOps.zCard(redisKey);
        if (currentDeviceCount != null && currentDeviceCount > MAX_ALLOWED_DEVICES) {
            // Neu so luong Device cua user vuot qua cho phep, tinh so luong can xoa truoc
            Long devicesToRemove = currentDeviceCount - MAX_ALLOWED_DEVICES;
            // Thuc hien xoa lan luot cac user:device co score thap nhat (tu 0 den devicesToRemove-1)
            zSetOps.removeRange(redisKey, 0, devicesToRemove - 1);
            log.info("Old device \"{}\" of user \"{}\" was deleted successfully", deviceFingerPrint, userId);
        }
        // Thiet lap thoi han song trong redis de don rac tu dong
        redisTemplate.expire(redisKey, DEVICE_EXPIRATION_DAYS, TimeUnit.DAYS);
    }
}
