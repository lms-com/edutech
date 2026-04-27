package com.lms.course.controller;

import com.lms.common.dto.response.ApiResponse;
import com.lms.course.dto.request.CategoryRequest;
import com.lms.course.dto.response.CategoryResponse;
import com.lms.course.service.CategoryService;
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

    @GetMapping
    public ApiResponse<List<CategoryResponse>> getAllCategories(@RequestParam(defaultValue = "false") boolean isTree){
        return ApiResponse.success(categoryService.getCategories(isTree));

    }

    @GetMapping("/{slug}")
    public ApiResponse<CategoryResponse> getCategoryBySlug(@PathVariable String slug){
        return ApiResponse.success(categoryService.getCategoryBySlug(slug));

    }

    @PostMapping
    public ApiResponse<CategoryResponse> createCategory(@RequestBody CategoryRequest request){
        return ApiResponse.success(categoryService.createCategory(request));

    }

    @PutMapping("/{id}")
    public ApiResponse<CategoryResponse> updateCategory(@PathVariable String id, @RequestBody CategoryRequest request){
        return ApiResponse.success(categoryService.updateCategory(id, request));

    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteCategory(@PathVariable String id){
        categoryService.deleteCategory(id);
        return ApiResponse.success("Xóa thành công!");

    }

    @PutMapping("/reorder")
    public ApiResponse<String> reorderCategories(@RequestBody List<String> orderedIds){
        categoryService.reorderCategories(orderedIds);
        return ApiResponse.success("Sắp xếp thành công!");

    }

}