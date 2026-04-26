package com.lms.iam.service;

import com.lms.common.exception.AppException;
import com.lms.iam.exception.IamErrorCode;
import com.lms.iam.model.Role;

import java.util.List;

public interface RoleService {

    void createNewRole (String roleName);

    List<Role> getAllRoles();

    Role getRoleDetails(String roleName);

    void deleteRole(String roleName);
}
