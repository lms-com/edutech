package com.lms.course.repository;

import com.lms.course.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SectionRepository extends JpaRepository<Section, String> {
    List<Section> findByCourseIdAndDeletedFalseOrderByOrderIndexAsc(String courseId);
}