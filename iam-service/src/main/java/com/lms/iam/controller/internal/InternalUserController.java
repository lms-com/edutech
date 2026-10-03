package com.lms.iam.controller.internal;

import com.lms.common.dto.response.ApiResponse;
import com.lms.iam.dto.response.LearnerInfoResponse;
import com.lms.iam.model.User;
import com.lms.iam.repository.UserRepository;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/internal/v1/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Hidden
public class InternalUserController {

    UserRepository userRepository;

    @GetMapping("/batch")
    public ApiResponse<List<LearnerInfoResponse>> getUsersByIds(@RequestParam("userIds") List<String> userIds) {
        log.info("InternalUserController: batch fetching profiles for userIds: {}", userIds);
        if (userIds == null || userIds.isEmpty()) {
            return ApiResponse.success(List.of());
        }

        List<User> users = userRepository.findAllById(userIds);
        List<LearnerInfoResponse> responseList = users.stream()
                .map(user -> LearnerInfoResponse.builder()
                        .id(user.getId())
                        .fullName(user.getFullName())
                        .email(user.getEmail())
                        .avatarUrl(null) // Cột avatarUrl chưa có thực tế trong bảng DB User, tạm thời trả về null hoặc custom placeholder
                        .build())
                .collect(Collectors.toList());

        log.info("InternalUserController: successfully fetched {} user profiles", responseList.size());
        return ApiResponse.success(responseList);
    }
}
