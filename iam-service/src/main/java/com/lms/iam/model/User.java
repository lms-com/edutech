package com.lms.iam.model;

import com.lms.common.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class User extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;
    @Column(name = "email", nullable = false, unique = true)
    String email;
    @Column(name = "password_hash", nullable = false)
    String password;
    @Column(name = "full_name")
    String fullName;
    @Column(name = "dob")
    LocalDate dob;
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    Userstatus status;
    @Column(name = "is_deleted")
    boolean isDeleted;

}
