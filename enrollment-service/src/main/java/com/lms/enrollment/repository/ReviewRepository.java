package com.lms.enrollment.repository;

import com.lms.enrollment.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, String> {
    List<Review> findAllByCourseId(String courseId);
    Page<Review> findAllByCourseId(String courseId, Pageable pageable);
    boolean existsByEnrollmentId(String enrollmentId);
    Optional<Review> findByEnrollmentId(String enrollmentId);

    @org.springframework.data.jpa.repository.Query(value = "SELECT * FROM reviews WHERE enrollment_id = :enrollmentId", nativeQuery = true)
    Optional<Review> findByEnrollmentIdIncludingDeleted(@org.springframework.data.repository.query.Param("enrollmentId") String enrollmentId);
}
