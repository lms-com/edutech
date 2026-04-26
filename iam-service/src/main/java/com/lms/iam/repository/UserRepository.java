package com.lms.iam.repository;

import com.lms.iam.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);

    @Query("""
        SELECT p.permissionKey FROM Permission p
        JOIN RolePermission rp ON p.id = rp.permissionId
        JOIN UserRole ur ON rp.roleId = ur.roleId
        where ur.userId = :userId AND p.isDeleted = FALSE AND rp.isDeleted = FALSE
    """)
    Set<String> findPermissionKeysByUserId (@Param("userId") String userId);

    @Query("""
        SELECT r.roleName FROM Role r
        JOIN UserRole ur ON r.id = ur.roleId
        WHERE ur.userId = :userId AND r.isDeleted = FALSE
    """)
    Set<String> findRoleNamesByUserId (@Param("userId") String userId);


    Optional<User> findUserById(String id);
}
