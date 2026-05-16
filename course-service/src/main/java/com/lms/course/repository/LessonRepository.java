package com.lms.course.repository;

import com.lms.course.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LessonRepository extends JpaRepository<Lesson, String> {
    List<Lesson> findBySectionIdAndDeletedFalse(String sectionId);
    List<Lesson> findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(String sectionId);
}