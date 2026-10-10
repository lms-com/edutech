package com.lms.iam.repository;

import com.lms.iam.model.User;
import com.lms.iam.model.Userstatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    List<User> findAll();

    @Query("""
                SELECT DISTINCT u from User u
                LEFT JOIN UserRole ur ON ur.userId = u.id
                LEFT JOIN Role r ON r.id = ur.roleId
                WHERE (:search IS NULL OR u.fullName LIKE %:search% OR u.email LIKE %:search%)
                AND (:status IS NULL OR u.status = :status)
                AND (:roleName IS NULL OR r.roleName = :roleName)
""")
    Page<User> findAllWithFilters (
            @Param("search") String search,
            @Param("status") Userstatus status,
            @Param("roleName") String roleName,
            Pageable pageable);


    @Query("""
            SELECT ur.userId, r.roleName FROM UserRole ur
            JOIN Role r ON ur.roleId = r.id
            WHERE ur.userId IN :userIds
""")
    List<Object[]> findRolesByUserIds (@Param("userIds") List<String> userIds);
}
