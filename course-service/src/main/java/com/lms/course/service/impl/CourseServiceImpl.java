package com.lms.course.service.impl;

import com.lms.common.exception.AppException;
import com.lms.course.dto.request.CourseRequest;
import com.lms.course.dto.response.CourseResponse;
import com.lms.course.entity.Category;
import com.lms.course.entity.Course;
import com.lms.course.exception.CourseErrorCode;
import com.lms.course.repository.CategoryRepository;
import com.lms.course.repository.CourseRepository;
import com.lms.course.service.CourseService;
import lombok.RequiredArgsConstructor;
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
        // 1. Kiểm tra trùng Slug sử dụng AppException
        if (courseRepository.existsBySlugAndNotDeleted(request.getSlug())) {
            throw new AppException(CourseErrorCode.COURSE_SLUG_EXISTS);
        }

        // 2. Kiểm tra Category có tồn tại không sử dụng AppException
        Category category = categoryRepository.findByIdAndIsDeletedFalse(request.getCategoryId())
                .orElseThrow(() -> new AppException(CourseErrorCode.CATEGORY_NOT_FOUND));

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
    @Override
    @Transactional(readOnly = true)
    public CourseResponse getCourseById(String courseId){
        Course course = courseRepository.findByIdAndNotDeleted(courseId)
                .orElseThrow(()->new AppException(CourseErrorCode.COURSE_NOT_FOUND));
        return mapToResponse(course);

    }

    private CourseResponse mapToResponse(Course course) {
        if (course == null) {
            return null;
        }

        return CourseResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .slug(course.getSlug())
                .instructorId(course.getInstructorId())
                // Tránh NullPointerException khi truy xuất bảng Category
                .categoryId(course.getCategory() != null ? course.getCategory().getId() : null)
                .categoryName(course.getCategory() != null ? course.getCategory().getName() : null)
                .description(course.getDescription())
                .thumbnailUrl(course.getThumbnailUrl())
                .level(course.getLevel())
                .basePrice(course.getBasePrice())
                .currencyCode(course.getCurrencyCode())
                .status(course.getStatus())
                // Các trường AuditableEntity (Tuỳ thuộc bạn có khai báo ở DTO không)
                // .createdAt(course.getCreatedAt())
                // .updatedAt(course.getUpdatedAt())
                .build();
    }
}