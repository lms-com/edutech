package com.lms.iam.repository;

import com.lms.iam.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, String> {

    @Query("""
            SELECT COUNT(ur) > 0 FROM UserRole ur
            JOIN Role r ON ur.roleId = r.id
            WHERE ur.userId = :userId AND r.roleName = :roleName
""")
    boolean existsByUserIdAndRoleName(@Param("userId") String userId, @Param("roleName") String roleName);
}
