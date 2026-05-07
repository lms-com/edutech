package com.lms.course.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL) // Ẩn field children nếu nó bị null (giúp JSON gọn đẹp)
public class CategoryResponse {
    String id;
    String name;
    String slug;
    String parentId;
    Integer orderIndex;
    List<CategoryResponse> children; // Mảng chứa các danh mục con
}