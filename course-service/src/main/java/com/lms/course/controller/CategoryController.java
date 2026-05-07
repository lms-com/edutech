package com.lms.course.controller;

import com.lms.common.dto.response.ApiResponse;
import com.lms.course.dto.request.CategoryRequest;
import com.lms.course.dto.response.CategoryResponse;
import com.lms.course.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Category Controller", description = "Quản lý danh mục khóa học")
public class CategoryController {
    CategoryService categoryService;

    @Operation(summary = "1. Lấy danh sách danh mục", description = "Lấy danh sách danh mục (Hỗ trợ param is_tree).")
    @GetMapping
    public ApiResponse<List<CategoryResponse>> getAllCategories(@RequestParam(defaultValue = "false") boolean isTree){
        return ApiResponse.success(categoryService.getCategories(isTree));

    }

    @Operation(summary = "2. Lấy chi tiết một danh mục", description = "Lấy chi tiết một danh mục (Phục vụ SEO).")
    @GetMapping("/{slug}")
    public ApiResponse<CategoryResponse> getCategoryBySlug(@PathVariable String slug){
        return ApiResponse.success(categoryService.getCategoryBySlug(slug));

    }

    @Operation(summary = "3. Tạo danh mục mới", description = "Tạo danh mục mới (Hỗ trợ parent_id).")
    @PostMapping
    public ApiResponse<CategoryResponse> createCategory(@RequestBody CategoryRequest request){
        return ApiResponse.success(categoryService.createCategory(request));

    }

    @Operation(summary = "4. Cập nhật thông tin danh mục", description = "Cập nhật thông tin danh mục.")
    @PutMapping("/{id}")
    public ApiResponse<CategoryResponse> updateCategory(@PathVariable String id, @RequestBody CategoryRequest request){
        return ApiResponse.success(categoryService.updateCategory(id, request));

    }

    @Operation(summary = "5. Xóa mềm danh mục", description = "Xóa mềm danh mục.")
    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteCategory(@PathVariable String id){
        categoryService.deleteCategory(id);
        return ApiResponse.success("Xóa thành công!");

    }

    @Operation(summary = "6. Sắp xếp lại thứ tự hiển thị danh mục", description = "Sắp xếp lại thứ tự hiển thị danh mục.")
    @PutMapping("/reorder")
    public ApiResponse<String> reorderCategories(@RequestBody List<String> orderedIds){
        categoryService.reorderCategories(orderedIds);
        return ApiResponse.success("Sắp xếp thành công!");

    }

}