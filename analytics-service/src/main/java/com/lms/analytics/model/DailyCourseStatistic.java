package com.lms.analytics.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;


@Entity
@Table(
        name = "daily_course_statistics",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"course_id", "record_date"})
        }
)
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DailyCourseStatistic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "course_id", nullable = false)
    String courseId;

    @Column(name = "instructor_id", nullable = false)
    String instructorId;

    @Column(name = "record_date", nullable = false)
    LocalDate recordDate;

    @Column(name = "total_order_amount", precision = 15, scale = 2)
    BigDecimal totalOrderAmount = BigDecimal.ZERO;

    @Column(name = "instructor_revenue", precision = 15, scale = 2)
    BigDecimal instructorRevenue = BigDecimal.ZERO;

    @Column(name = "platform_fee", precision = 15, scale = 2)
    BigDecimal platformFee = BigDecimal.ZERO;

    @Column(name = "daily_enrollments")
    Integer dailyEnrollments = 0;

    @Column(name = "refunded_amount", precision = 15, scale = 2)
    BigDecimal refundedAmount = BigDecimal.ZERO;

    @Column(name = "refunded_enrollments")
    Integer refundedEnrollments = 0;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    Instant updatedAt;
}
