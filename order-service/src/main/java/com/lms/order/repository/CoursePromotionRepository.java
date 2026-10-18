package com.lms.order.repository;

import com.lms.order.model.CoursePromotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CoursePromotionRepository extends JpaRepository<CoursePromotion, String> {

    boolean existsByPromotionIdAndCourseId(String promotionId, String courseId);

    List<CoursePromotion> findByPromotionId(String promotionId);

    void deleteByPromotionIdAndCourseId(String promotionId, String courseId);
}
