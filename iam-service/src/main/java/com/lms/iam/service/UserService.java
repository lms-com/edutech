package com.lms.iam.service;

import com.lms.iam.dto.response.UserProfileReponse;

public interface UserService {

    UserProfileReponse getUserProfile(String userId);
}
