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
    private final com.lms.iam.repository.UserRepository userRepository;

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
        Double score = redisTemplate.opsForZSet().score(redisKey, deviceFingerPrint);
        return score != null;
    }

    @Override
    public void addToBlackList(String userId, String deviceFingerPrint) {
        if (existsInBlackList(userId, deviceFingerPrint))
            return;
        String redisKey = getUserBlackListRedisKey(userId);
        redisTemplate.opsForZSet().add(redisKey, deviceFingerPrint, System.currentTimeMillis());
        redisTemplate.expire(redisKey, DEVICE_EXPIRATION_DAYS, TimeUnit.DAYS);
    }

    @Override
    public void deleteAllDevicesOfUser(String userId) {
        String redisKey = getUserDeviceRedisKey(userId);
        redisTemplate.delete(redisKey);
        log.info("All devices of user {} were deleted successfully", userId);
    }

    @Override
    public List<com.lms.iam.dto.response.AdminDeviceResponse> getAllActiveDevices(String search) {
        List<com.lms.iam.dto.response.AdminDeviceResponse> result = new java.util.ArrayList<>();
        List<com.lms.iam.model.User> users;
        if (search != null && !search.trim().isEmpty()) {
            users = userRepository.findAll().stream()
                    .filter(u -> (u.getEmail() != null && u.getEmail().toLowerCase().contains(search.toLowerCase()))
                            || (u.getFullName() != null && u.getFullName().toLowerCase().contains(search.toLowerCase())))
                    .toList();
        } else {
            users = userRepository.findAll();
        }

        for (com.lms.iam.model.User user : users) {
            String redisKey = getUserDeviceRedisKey(user.getId());
            Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet().reverseRangeWithScores(redisKey, 0, -1);
            if (tuples != null && !tuples.isEmpty()) {
                for (ZSetOperations.TypedTuple<String> tuple : tuples) {
                    if (tuple.getValue() == null || tuple.getScore() == null) continue;
                    long epochMilli = tuple.getScore().longValue();
                    LocalDateTime loginAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZoneId.systemDefault());
                    boolean blocked = existsInBlackList(user.getId(), tuple.getValue());

                    long diffMinutes = (System.currentTimeMillis() - epochMilli) / (60 * 1000);
                    String lastActiveStr;
                    if (diffMinutes < 1) {
                        lastActiveStr = "Vừa xong";
                    } else if (diffMinutes < 60) {
                        lastActiveStr = diffMinutes + " phút trước";
                    } else if (diffMinutes < 1440) {
                        lastActiveStr = (diffMinutes / 60) + " giờ trước";
                    } else {
                        lastActiveStr = (diffMinutes / 1440) + " ngày trước";
                    }

                    result.add(com.lms.iam.dto.response.AdminDeviceResponse.builder()
                            .deviceId(tuple.getValue())
                            .deviceFingerprint(tuple.getValue())
                            .userId(user.getId())
                            .userEmail(user.getEmail())
                            .userFullName(user.getFullName())
                            .loginAt(loginAt)
                            .lastActive(lastActiveStr)
                            .isBlocked(blocked)
                            .build());
                }
            }
        }
        return result;
    }

    @Override
    public void revokeDevice(String userId, String deviceFingerprint) {
        deleteUserDevice(userId, deviceFingerprint);
        addToBlackList(userId, deviceFingerprint);
        log.info("Admin revoked and blacklisted device {} of user {}", deviceFingerprint, userId);
    }
}
