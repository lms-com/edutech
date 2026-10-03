package com.lms.iam.service.impl;

import com.lms.common.exception.AppException;
import com.lms.iam.dto.request.UpdateUserStatusRequest;
import com.lms.iam.dto.response.UserProfileReponse;
import com.lms.iam.dto.response.UserResponse;
import com.lms.iam.exception.IamErrorCode;
import com.lms.iam.model.User;
import com.lms.iam.model.Userstatus;
import com.lms.iam.repository.InstructorProfileRepository;
import com.lms.iam.repository.LearnerProfileRepository;
import com.lms.iam.repository.UserRepository;
import com.lms.iam.service.DeviceManagementService;
import com.lms.iam.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final LearnerProfileRepository learnerProfileRepository;
    private final InstructorProfileRepository instructorProfileRepository;
    private final DeviceManagementService deviceManagementService;

    @Override
    public boolean existsByUserId(String userId) {
        return userRepository.existsById(userId);
    }


    @Override
    public UserProfileReponse getUserProfile(String userId) {
        // Lay thong tin chung cua User
        User user = userRepository.findUserById(userId)
                .orElseThrow(() -> new AppException(
                        IamErrorCode.USER_NOT_EXISTED,
                        String.format("User id %s not found", userId)
                ));

        // Lay Roles va Permissons cua User
        Set<String> userRoles = userRepository.findRoleNamesByUserId(userId);
        Set<String> userPermissions = userRepository.findPermissionKeysByUserId(userId);

        Object userProfile = null;
        if (userRoles.contains("INSTRUCTOR")) {
            userProfile = instructorProfileRepository.findInstructorProfileByUserId(userId).orElse(null);
        }
        if (userRoles.contains("LEARNER")) {
            userProfile = learnerProfileRepository.findLearnerProfileByUserId(userId)
                    .orElse(null);
        }

        return UserProfileReponse.builder()
                .userId(userId)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .dob(user.getDob())
                .roles(userRoles)
                .permissions(userPermissions)
                .profile(userProfile)
                .build();
    }


    @Override
    public Page<UserResponse> getAllUsers(String search, Userstatus status, String roleName, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        // Lay danh sach cac user vao page
        Page<User> userPage = userRepository.findAllWithFilters(search, status, roleName, pageable);
        // Lay Danh sach userIds de lay danh sach user:role tuong ung
        List<String> userIds = userPage.getContent().stream()
                .map(User::getId)
                .toList();

        // Lay Danh sach User:Role
        List<Object[]> userRoleData = userRepository.findRolesByUserIds(userIds);

        // Dung Map de nhom Role (Value) theo UserId (Key)
        Map<String, Set<String>> rolesMap = new HashMap<>();

        for (Object[] data : userRoleData) {
            String userId = (String) data[0];
            String role = (String) data[1];
            rolesMap.computeIfAbsent(userId, k -> new HashSet<>()).add(role);
        }

        // Map tu Page<User> thanh Page<UserResponse>
        return userPage.map(user ->
            UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .status(user.getStatus())
                .roles(rolesMap.getOrDefault(user.getEmail(), new HashSet<>()))
                .build()
        );
    }


    @Override
    @Transactional
    public void updateUserStatus(String userId, UpdateUserStatusRequest request) {
        User user = userRepository.findUserById(userId)
                .orElseThrow(() -> new AppException(IamErrorCode.USER_NOT_EXISTED, String.format("User id {} not found", userId)));

        user.setStatus(request.getStatus());
        userRepository.save(user);

        if (!request.getStatus().equals(Userstatus.ACTIVE)) {
            deviceManagementService.deleteAllDevicesOfUser(userId);
        }
    }
}
