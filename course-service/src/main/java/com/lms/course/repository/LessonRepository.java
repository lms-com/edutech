package com.lms.course.repository;

import com.lms.course.entity.Lesson;
import com.lms.course.entity.VideoLesson;
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

    // Internal API 42: Tìm các bài giảng video gắn với mediaId
    @Query("SELECT v FROM VideoLesson v JOIN FETCH v.section s JOIN FETCH s.course c " +
           "WHERE v.deleted = false AND c.deleted = false " +
           "AND (v.videoUrl = :mediaId OR v.videoUrl LIKE CONCAT('%/', :mediaId, '/view%'))")
    List<VideoLesson> findVideoLessonsByMediaId(@Param("mediaId") String mediaId);
}