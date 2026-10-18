package com.lms.course.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CategoryRequest {
    @NotBlank(message = "Tên danh mục không được để trống")
    String name;

    @NotBlank(message = "Slug danh mục không được để trống")
    String slug;

    String parentId; // Có thể null nếu là danh mục gốc
}