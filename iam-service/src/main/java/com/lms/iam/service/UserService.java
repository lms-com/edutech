package com.lms.iam.service;

import com.lms.iam.dto.response.UserProfileReponse;

import java.util.List;

public interface UserService {

    UserProfileReponse getUserProfile(String userId);

}
