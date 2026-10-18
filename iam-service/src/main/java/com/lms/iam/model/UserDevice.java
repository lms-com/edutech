package com.lms.iam.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_devices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserDevice {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;
    @Column(name = "user_id", nullable = false)
    String userId;
    @Column(name = "device_fingerprint", nullable = false)
    String deviceFingerprint;
    @Column(name = "device_name")
    String deviceName;
    @Column(name = "is_active")
    boolean isActive;
    @Column(name = "last_login")
    LocalDateTime lastLogin;
}
