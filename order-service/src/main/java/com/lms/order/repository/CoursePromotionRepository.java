package com.lms.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CoursePromotionRepository extends JpaRepository<CoursePromotionRepository, String> {

    boolean existsByPromotionIdAndCourseId(String promotionId, String courseId);
}
