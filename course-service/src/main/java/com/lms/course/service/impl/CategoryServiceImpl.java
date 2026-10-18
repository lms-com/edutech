package com.lms.course.service.impl;

import com.lms.common.exception.AppException;
import com.lms.course.dto.request.CategoryRequest;
import com.lms.course.dto.response.CategoryResponse;
import com.lms.course.entity.Category;
import com.lms.course.exception.CourseErrorCode;
import com.lms.course.repository.CategoryRepository;
// Bạn nhớ tạo CategoryService interface có các hàm tương ứng nhé
import com.lms.course.service.CategoryService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CategoryServiceImpl implements CategoryService {

    CategoryRepository categoryRepository;

    @Override
    public List<CategoryResponse> getCategories(boolean isTree) {
        List<Category> categories = categoryRepository.findByDeletedFalseOrderByOrderIndexAsc();

        // Nếu người dùng không muốn lấy dạng cây, trả về danh sách phẳng
        if (!isTree) {
            return categories.stream().map(this::mapToResponse).toList();
        }

        // Thuật toán xây dựng cấu trúc cây (O(N) hiệu năng cao)
        // 1. Gom nhóm các danh mục con theo parentId
        Map<String, List<Category>> childrenMap = categories.stream()
                .filter(c -> c.getParentId() != null)
                .collect(Collectors.groupingBy(Category::getParentId));

        // 2. Lấy các danh mục gốc (parentId == null) và đệ quy gắn con cho nó
        return categories.stream()
                .filter(c -> c.getParentId() == null)
                .map(c -> buildTree(c, childrenMap))
                .toList();
    }

    @Override
    public CategoryResponse getCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlugAndDeletedFalse(slug)
                .orElseThrow(() -> new AppException(CourseErrorCode.CATEGORY_NOT_FOUND));
        return mapToResponse(category);
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.existsBySlug(request.getSlug())) {
            throw new AppException(CourseErrorCode.CATEGORY_SLUG_EXISTS);
        }

        Category category = new Category();
        category.setName(request.getName());
        category.setSlug(request.getSlug());
        category.setParentId(request.getParentId());
        // Có thể lấy max orderIndex + 1 ở đây, tạm thời gán 0
        category.setOrderIndex(0);

        return mapToResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(String id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(CourseErrorCode.CATEGORY_NOT_FOUND));

        if (categoryRepository.existsBySlugAndIdNot(request.getSlug(), id)) {
            throw new AppException(CourseErrorCode.CATEGORY_SLUG_EXISTS);
        }

        category.setName(request.getName());
        category.setSlug(request.getSlug());
        category.setParentId(request.getParentId());

        return mapToResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public void deleteCategory(String id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(CourseErrorCode.CATEGORY_NOT_FOUND));
        // Xóa mềm: đánh dấu is_deleted = true và đổi slug để giải phóng slug cho danh mục khác
        category.setDeleted(true);
        category.setSlug(category.getSlug() + "-deleted-" + System.currentTimeMillis());
        categoryRepository.save(category);
    }

    @Override
    @Transactional
    public void reorderCategories(List<String> orderedIds) {
        for (int i = 0; i < orderedIds.size(); i++) {
            String id = orderedIds.get(i);
            Category category = categoryRepository.findById(id).orElse(null);
            if (category != null) {
                category.setOrderIndex(i);
                categoryRepository.save(category);
            }
        }
    }

    // --- Hàm phụ trợ ---

    private CategoryResponse buildTree(Category category, Map<String, List<Category>> childrenMap) {
        CategoryResponse response = mapToResponse(category);
        List<Category> children = childrenMap.get(category.getId());

        if (children != null && !children.isEmpty()) {
            response.setChildren(children.stream()
                    .map(child -> buildTree(child, childrenMap)) // Đệ quy
                    .toList());
        }
        return response;
    }

    private CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .parentId(category.getParentId())
                .orderIndex(category.getOrderIndex())
                .build();
    }
}