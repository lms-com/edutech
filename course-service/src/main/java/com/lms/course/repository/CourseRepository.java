package com.lms.course.repository;

import com.lms.course.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, String> {
    // 1. Lấy chi tiết Course (Dùng cho getCourseById cơ bản)
    @Query("SELECT c FROM Course c WHERE c.id = :id AND c.deleted = false")
    Optional<Course> findByIdAndNotDeleted(@Param("id") String id);

    // 1b. Lấy chi tiết Course kèm cấu trúc chương trình học (chống N+1 query)
    @org.springframework.data.jpa.repository.EntityGraph(value = "course.withCurriculum", type = org.springframework.data.jpa.repository.EntityGraph.EntityGraphType.LOAD)
    @Query("SELECT c FROM Course c WHERE c.id = :id AND c.deleted = false")
    Optional<Course> findByIdWithCurriculum(@Param("id") String id);
    // 2. tim kiem slug
    @Query("SELECT c FROM Course c WHERE c.slug = :slug AND c.deleted = false")
    Optional<Course> findBySlugAndNotDeleted(@Param("slug") String slug);

    //3. Tìm tất cả Course
    @Query("SELECT c FROM Course c WHERE c.deleted = false")
    Page<Course> findAllByNotDeleted(Pageable pageable);


    // 4. Kiểm tra trùng Slug (Dùng cho create/update)
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN TRUE ELSE FALSE END FROM Course c WHERE c.slug = :slug AND c.deleted = false")
    boolean existsBySlugAndNotDeleted(@Param("slug") String slug);

    // 5. Lấy danh sách Course phân trang (Dành cho User/Hệ thống)
    @Query("SELECT c FROM Course c WHERE c.deleted = false")
    Page<Course> findAllNotDeleted(Pageable pageable);

    // 6. Lấy danh sách Course theo Giảng viên (Dành cho Instructor Dashboard)
    @Query("SELECT c FROM Course c WHERE c.instructorId = :instructorId AND c.deleted = false")
    Page<Course> findByInstructorIdAndNotDeleted(@Param("instructorId") String instructorId, Pageable pageable);

    // 7. Lấy danh sách các khóa học liên quan (Cross-sale)
    @Query(value = "SELECT c FROM Course c WHERE c.category.id = :categoryId AND c.id != :excludeCourseId AND c.status = 'PUBLISHED' AND c.deleted = false")
    Page<Course> findRelatedCoursesByCategoryId(@Param("categoryId") String categoryId, @Param("excludeCourseId") String excludeCourseId, Pageable pageable);

    // 8. Lấy danh sách Course cho Admin (có lọc theo status, instructorId)
    @Query("SELECT c FROM Course c WHERE c.deleted = false " +
           "AND (:status IS NULL OR c.status = :status) " +
           "AND (:instructorId IS NULL OR c.instructorId = :instructorId)")
    Page<Course> findAllForAdmin(@Param("status") String status, @Param("instructorId") String instructorId, Pageable pageable);

    // 9. Internal: Lấy nhiều khóa học theo danh sách ID (batch lookup)
    @Query("SELECT c FROM Course c WHERE c.id IN :ids AND c.deleted = false")
    List<Course> findAllByIdInAndNotDeleted(@Param("ids") List<String> ids);
}