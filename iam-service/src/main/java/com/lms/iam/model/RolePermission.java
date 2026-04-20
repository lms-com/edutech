package com.lms.iam.model;

import com.lms.common.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "role_permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class RolePermission extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;
    @Column(name = "role_id", nullable = false)
    String role_id;
    @Column(name = "permission_id", nullable = false)
    String permission_id;
    @Column(name = "is_deleted")
    boolean isDeleted;
}
