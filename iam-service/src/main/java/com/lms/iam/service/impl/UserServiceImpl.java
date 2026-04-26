package com.lms.iam.service.impl;

import com.lms.common.exception.AppException;
import com.lms.iam.dto.response.UserProfileReponse;
import com.lms.iam.exception.IamErrorCode;
import com.lms.iam.model.User;
import com.lms.iam.repository.InstructorProfileRepository;
import com.lms.iam.repository.LearnerProfileRepository;
import com.lms.iam.repository.UserRepository;
import com.lms.iam.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final LearnerProfileRepository learnerProfileRepository;
    private final InstructorProfileRepository instructorProfileRepository;

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
}
