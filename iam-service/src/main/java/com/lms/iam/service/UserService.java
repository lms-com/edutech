package com.lms.iam.service;

import com.lms.iam.dto.response.UserProfileReponse;
import com.lms.iam.dto.response.UserResponse;
import com.lms.iam.model.Userstatus;
import org.springframework.data.domain.Page;

import java.util.List;

public interface UserService {

    boolean existsByUserId(String userId);

    UserProfileReponse getUserProfile(String userId);

    Page<UserResponse> getAllUsers (String search, Userstatus status, String roleName, int page, int size);
}
