package com.lms.iam.service.impl;

import com.lms.common.exception.AppException;
import com.lms.iam.exception.IamErrorCode;
import com.lms.iam.model.Role;
import com.lms.iam.repository.RoleRepository;
import com.lms.iam.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {
    private final RoleRepository roleRepository;

    @Override
    public void createNewRole(String roleName) {
        if (roleRepository.existsRoleByRoleName(roleName)) {
            throw new AppException(IamErrorCode.ROLE_ALREADY_EXISTS);
        }
        roleRepository.save(Role.builder().roleName(roleName).build());
    }

    @Override
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    @Override
    public Role getRoleDetails(String roleName) {
        return roleRepository.findRoleByRoleName(roleName)
                .orElseThrow(() -> new AppException(IamErrorCode.ROLE_NOT_FOUND));
    }

    @Override
    public void deleteRole(String roleName) {

    }
}
