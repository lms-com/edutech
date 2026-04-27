package com.lms.course.repository;

import com.lms.course.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, String> {
    // 1. Lấy chi tiết Course (Dùng cho getCourseById)
    @Query("SELECT c FROM Course c WHERE c.id = :id AND c.isDeleted = false")
    Optional<Course> findByIdAndNotDeleted(@Param("id") String id);
    // 2. tim kiem slug
    @Query("SELECT c FROM Course c WHERE c.slug = :slug AND c.isDeleted = false")
    Optional<Course> findBySlugAndNotDeleted(@Param("slug") String slug);

    //3. Tìm tất cả Course
    @Query("SELECT c FROM Course c WHERE c.isDeleted = false")
    Page<Course> findAllByNotDeleted(Pageable pageable);


    // 4. Kiểm tra trùng Slug (Dùng cho create/update)
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN TRUE ELSE FALSE END FROM Course c WHERE c.slug = :slug AND c.isDeleted = false")
    boolean existsBySlugAndNotDeleted(@Param("slug") String slug);

    // 5. Lấy danh sách Course phân trang (Dành cho User/Hệ thống)
    @Query("SELECT c FROM Course c WHERE c.isDeleted = false")
    Page<Course> findAllNotDeleted(Pageable pageable);

    // 6. Lấy danh sách Course theo Giảng viên (Dành cho Instructor Dashboard)
    @Query("SELECT c FROM Course c WHERE c.instructorId = :instructorId AND c.isDeleted = false")
    Page<Course> findByInstructorIdAndNotDeleted(@Param("instructorId") String instructorId, Pageable pageable);
}
