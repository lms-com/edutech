package com.lms.notification.repository;

import com.lms.notification.entity.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LessonProgressRepository extends JpaRepository<LessonProgress, String> {
    Optional<LessonProgress> findByEnrollmentIdAndLessonId(String enrollmentId, String lessonId);
    List<LessonProgress> findAllByEnrollmentId(String enrollmentId);
    long countByEnrollmentIdAndIsCompletedTrue(String enrollmentId);
}
