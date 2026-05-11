package com.lms.course.repository;

import com.lms.course.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, String> {
    // Chỉ lấy danh mục chưa bị xóa mềm, sắp xếp theo orderIndex
    List<Category> findByDeletedFalseOrderByOrderIndexAsc();

    // Lấy chi tiết theo Slug
    Optional<Category> findBySlugAndDeletedFalse(String slug);

    Optional<Category> findByIdAndDeletedFalse(String id);

    // Kiểm tra trùng Slug
    boolean existsBySlugAndIdNot(String slug, String id);
    boolean existsBySlug(String slug);
}