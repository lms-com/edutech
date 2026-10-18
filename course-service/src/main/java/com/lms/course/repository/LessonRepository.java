package com.lms.course.repository;

import com.lms.course.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LessonRepository extends JpaRepository<Lesson, String> {
    List<Lesson> findBySectionIdAndDeletedFalse(String sectionId);
    List<Lesson> findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(String sectionId);

    // Internal: Tìm lesson chưa bị xóa theo ID
    Optional<Lesson> findByIdAndDeletedFalse(String lessonId);

    // Internal API 37: Đếm tổng bài học theo danh sách section IDs
    @Query("SELECT COUNT(l) FROM Lesson l WHERE l.section.id IN :sectionIds AND l.deleted = false")
    long countBySectionIdsAndNotDeleted(@Param("sectionIds") List<String> sectionIds);
}