package com.lms.course.repository;

import com.lms.course.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, String> {
    boolean existsBySlugAndIsDeletedFalse(String slug);
    Optional<Course> findByIdAndIsDeletedFalse(String id);
}