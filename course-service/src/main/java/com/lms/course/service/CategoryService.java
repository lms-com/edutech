package com.lms.course.service;

import com.lms.course.dto.request.CategoryRequest;
import com.lms.course.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryService {

    // Lấy danh sách danh mục (Hỗ trợ trả về dạng phẳng hoặc dạng cây)
    List<CategoryResponse> getCategories(boolean isTree);

    // Lấy chi tiết một danh mục bằng Slug (Dùng cho SEO Frontend)
    CategoryResponse getCategoryBySlug(String slug);

    // Tạo danh mục mới
    CategoryResponse createCategory(CategoryRequest request);

    // Cập nhật thông tin danh mục
    CategoryResponse updateCategory(String id, CategoryRequest request);

    // Xóa mềm danh mục
    void deleteCategory(String id);

    // Cập nhật lại thứ tự hiển thị của các danh mục
    void reorderCategories(List<String> orderedIds);

}