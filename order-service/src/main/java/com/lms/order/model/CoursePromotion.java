package com.lms.order.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "course_promotions")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CoursePromotion {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id", nullable = false)
    Promotion promotion;

    @Column(name = "course_id", length = 36, nullable = false)
    String courseId;
}
