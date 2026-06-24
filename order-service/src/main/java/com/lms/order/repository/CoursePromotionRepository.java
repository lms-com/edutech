package com.lms.order.repository;

import com.lms.order.model.CoursePromotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CoursePromotionRepository extends JpaRepository<CoursePromotion, String> {

    boolean existsByPromotionIdAndCourseId(String promotionId, String courseId);
}
