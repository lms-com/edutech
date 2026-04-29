package com.lms.iam.service;

import com.lms.iam.dto.response.UserProfileReponse;

import java.util.List;

public interface UserService {

    boolean existsByUserId(String userId);

    UserProfileReponse getUserProfile(String userId);

}
