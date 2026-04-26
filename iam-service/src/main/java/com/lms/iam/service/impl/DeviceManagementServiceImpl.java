package com.lms.iam.service.impl;

import com.lms.iam.dto.response.UserDeviceResponse;
import com.lms.iam.service.DeviceManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceManagementServiceImpl implements DeviceManagementService {
    private final StringRedisTemplate redisTemplate;

    @Override
    public void registerDevice(String userId, String deviceFingerPrint) {
        // Tao key theo userId trong redis cho cap user:device
        String redisKey = getUserDeviceRedisKey(userId);
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


    @Override
    public List<UserDeviceResponse> getUserDevices(String userId) {
        String redisKey = getUserDeviceRedisKey(userId);

        // Lay danh sach (deviceFingerPrint, score) tu moi den cu
        Set<ZSetOperations.TypedTuple<String>> typedTuples = redisTemplate
                .opsForZSet().reverseRangeWithScores(redisKey, 0, -1);

        if (typedTuples == null) return List.of();

        return typedTuples.stream()
                .map(tuple -> {
                    // Chuyen score thanh LocalDateTime
                    LocalDateTime loginAt = LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(tuple.getScore().longValue()),
                            ZoneId.systemDefault()
                    );
                    return UserDeviceResponse.builder()
                            .deviceFingerprint(tuple.getValue())
                            .loginAt(loginAt)
                            .build();
                })
                .collect(Collectors.toList());
    }


    @Override
    public String getUserDeviceRedisKey(String userId) {
        return "user:" + userId + ":device";
    }


    @Override
    public String getUserBlackListRedisKey(String userId) {
        return "user:" + userId + ":blacklist";
    }


    @Override
    public void deleteUserDevice(String userId, String deviceFingerPrint) {
        String redisKey = getUserDeviceRedisKey(userId);
        redisTemplate.opsForZSet().remove(redisKey, deviceFingerPrint);
        log.info("Device {} was deleted successfully", deviceFingerPrint);
    }


    @Override
    public boolean existsInBlackList(String userId, String deviceFingerPrint) {
        String redisKey = getUserBlackListRedisKey(userId);
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(redisKey, deviceFingerPrint));
    }

    @Override
    public void addToBlackList(String userId, String deviceFingerPrint) {
        if (existsInBlackList(userId, deviceFingerPrint))
            return;
        String redisKey = getUserBlackListRedisKey(userId);
        redisTemplate.opsForZSet().add(redisKey, deviceFingerPrint, System.currentTimeMillis());
    }
}
