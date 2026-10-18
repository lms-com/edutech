package com.lms.iam.model;

import com.lms.common.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name="permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Permission extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;
    @Column(name = "permission_key", nullable = false, unique = true)
    String permissionKey;
    @Column(name = "resource_group")
    String resourceGroup;
    @Column(name="is_deleted")
    boolean isDeleted;
}
