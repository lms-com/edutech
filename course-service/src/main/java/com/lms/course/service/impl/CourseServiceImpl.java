package com.lms.course.service.impl;

import com.lms.common.exceptions.BusinessException;
import com.lms.course.dto.request.CourseRequest;
import com.lms.course.dto.response.CourseResponse;
import com.lms.course.entity.Category;
import com.lms.course.entity.Course;
import com.lms.course.repository.CategoryRepository;
import com.lms.course.repository.CourseRepository;
import com.lms.course.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public CourseResponse createCourse(CourseRequest request, String instructorId) {
        // 1. Kiểm tra trùng Slug
        if (courseRepository.existsBySlugAndIsDeletedFalse(request.getSlug())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Slug khóa học đã tồn tại");
        }

        // 2. Kiểm tra Category có tồn tại không
        Category category = categoryRepository.findByIdAndIsDeletedFalse(request.getCategoryId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy danh mục"));

        // 3. Map sang Entity
        Course course = Course.builder()
                .title(request.getTitle())
                .slug(request.getSlug())
                .category(category)
                .instructorId(instructorId) // Logical ID từ IAM Service
                .description(request.getDescription())
                .thumbnailUrl(request.getThumbnailUrl())
                .level(request.getLevel())
                .basePrice(request.getBasePrice())
                .currencyCode(request.getCurrencyCode() != null ? request.getCurrencyCode() : "VND")
                .status("DRAFT")
                .build();

        course = courseRepository.save(course);

        // 4. Trả về Response
        return CourseResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .slug(course.getSlug())
                .instructorId(course.getInstructorId())
                .categoryName(category.getName())
                .basePrice(course.getBasePrice())
                .currencyCode(course.getCurrencyCode())
                .status(course.getStatus())
                .build();
    }
}