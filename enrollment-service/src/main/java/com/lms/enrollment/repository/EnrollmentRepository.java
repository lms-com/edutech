package com.lms.enrollment.repository;

import com.lms.enrollment.entity.Enrollment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, String> {
    Optional<Enrollment> findByLearnerIdAndCourseId(String learnerId, String courseId);
    List<Enrollment> findAllByLearnerId(String learnerId);
    Page<Enrollment> findAllByLearnerId(String learnerId, Pageable pageable);
    boolean existsByLearnerIdAndCourseId(String learnerId, String courseId);
}
