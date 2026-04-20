package com.lms.iam.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Entity
@Table(name = "instructor_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InstructorProfile {
    @Id
    @Column(name = "user_id")
    String userId;
    @Column(name = "bio", columnDefinition = "TEXT")
    String bio;
    @Column(name = "default_commission_rate", precision = 3, scale = 2)
    BigDecimal defaultCommissionRate;
}
