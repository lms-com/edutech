package com.lms.iam.controller;

import com.lms.common.dto.response.ApiResponse;
import com.lms.common.exception.AppException;
import com.lms.iam.dev.DevUpdateUserPasswordRequest;
import com.lms.iam.dev.DevUserRoleRequest;
import com.lms.iam.dto.response.UserProfileReponse;
import com.lms.iam.exception.IamErrorCode;
import com.lms.iam.model.Role;
import com.lms.iam.model.User;
import com.lms.iam.model.UserRole;
import com.lms.iam.model.Userstatus;
import com.lms.iam.repository.UserRepository;
import com.lms.iam.repository.UserRoleRepository;
import com.lms.iam.service.RoleService;
import com.lms.iam.service.UserService;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Profile("dev")
@RestController
@RequestMapping("/dev")
@RequiredArgsConstructor
public class DevController {

    private final RoleService roleService;
    private final UserService userService;

    private final UserRoleRepository userRoleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/users")
    public ApiResponse<List<User>> devGetAllUsers() {
        return ApiResponse.success(
                userRepository.findAll()
        );
    }

    @PostMapping("/user/info")
    public ApiResponse<UserProfileReponse> devGetUserProfile(@RequestBody String userId) {
        return ApiResponse.success(userService.getUserProfile(userId));
    }

    @GetMapping("/roles")
    public ApiResponse<List<Role>> devGetRoles() {
        return ApiResponse.success(
                roleService.getAllRoles(),
                "Dev get all roles successfully"
        );
    }

    @PostMapping("/user-role")
    public ApiResponse<?> devSetRoleForUser (@RequestBody DevUserRoleRequest request){
        String userId = request.getUserId();
        if (!userService.existsByUserId(userId)) {
            throw new AppException(IamErrorCode.USER_NOT_EXISTED,
                    String.format("User Id: %s doesn't exist", userId));
        }

        // Kiem tra user da duoc gan roleName chua
        String roleName = request.getRoleName();
        if (userRoleRepository.existsByUserIdAndRoleName(userId, roleName)) {
            return ApiResponse.success(
                    String.format("User {} already has set role {}", userId, roleName)
            );
        }

        // Gan roleName cho User
            // Lay roleId tu roleName de gan vao UserRole
        Role role = roleService.getRoleDetails(roleName);
        UserRole userRole = UserRole.builder()
                .userId(userId)
                .roleId(role.getId())
                .build();
        userRoleRepository.save(userRole);
        return ApiResponse.success(
                "Set user role successfully"
        );
    }


    @PatchMapping("/update/role/user")
    public ApiResponse<?> devUpdateRoleOfUser (@RequestBody DevUserRoleRequest request) {
        String userId = request.getUserId();

        // Kiem tra trong UserRole truoc
        if (userRoleRepository.existsByUserIdAndRoleName(userId, request.getRoleName())) {
            return ApiResponse.success(
                    String.format("User {} already has set role {}", userId, request.getRoleName()));
        }

        User user = userRepository.findUserById(userId)
                .orElseThrow(() -> new AppException(IamErrorCode.USER_NOT_EXISTED, "Not found userId " + userId));

        Role role = roleService.getRoleDetails(request.getRoleName());

        UserRole newUserRole = userRoleRepository.save(
                UserRole.builder()
                        .userId(userId)
                        .roleId(role.getId())
                        .build()
        );

        return ApiResponse.success(
                newUserRole,
                String.format("Set new role %s for user $d successfully", request.getRoleName(), userId)
        );
    }


    @PatchMapping("/update/password/user")
    public ApiResponse<User> devUpdateUserPassword (@RequestBody DevUpdateUserPasswordRequest request) {
        String userId = request.getUserId();
        User user = userRepository.findUserById(userId)
                .orElseThrow(() -> new AppException(IamErrorCode.USER_NOT_EXISTED, "Not found userId " + userId));
        String hashedPassword = passwordEncoder.encode(request.getNewPassword());
        user.setPassword(hashedPassword);
        userRepository.save(user);
        return ApiResponse.success(
                user,
                String.format("Change password for user $d successfully", userId)
        );
    }
}
