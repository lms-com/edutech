package com.lms.course.repository; // Đảm bảo đúng package

import com.lms.course.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository // Thêm annotation này vào
public interface CategoryRepository extends JpaRepository<Category, String> {
    // ...
    // Lấy Category theo ID và đảm bảo chưa bị xóa mềm
    @Query("SELECT c FROM Category c WHERE c.id = :id AND c.isDeleted = false")
    Optional<Category> findByIdAndIsDeletedFalse(@Param("id") String id);

}