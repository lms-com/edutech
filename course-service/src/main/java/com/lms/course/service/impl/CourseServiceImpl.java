package com.lms.course.service.impl;

import com.lms.common.exception.AppException;
import com.lms.course.dto.request.CourseRequest;
import com.lms.course.dto.request.CourseUpdateRequest;
import com.lms.course.dto.response.CourseDetailResponse;
import com.lms.course.dto.response.CourseResponse;
import com.lms.course.dto.response.LessonResponse;
import com.lms.course.dto.response.SectionResponse;
import com.lms.course.entity.*;
import com.lms.course.exception.CourseErrorCode;
import com.lms.course.repository.CategoryRepository;
import com.lms.course.repository.CourseRepository;
import com.lms.course.repository.LessonRepository;
import com.lms.course.repository.SectionRepository;
import com.lms.course.service.CourseService;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final CategoryRepository categoryRepository;
    private final SectionRepository sectionRepository;
    private final LessonRepository lessonRepository;

    @Override
    @Transactional
    public CourseResponse createCourse(CourseRequest request, String instructorId) {
        // 1. Kiểm tra trùng Slug sử dụng AppException
        if (courseRepository.existsBySlugAndNotDeleted(request.getSlug())) {
            throw new AppException(CourseErrorCode.COURSE_SLUG_EXISTS);
        }

        // 2. Kiểm tra Category có tồn tại không sử dụng AppException
        Category category = categoryRepository.findByIdAndDeletedFalse(request.getCategoryId())
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

        // 4. Lưu
        course = courseRepository.save(course);

        // 5. Trả về Response bằng cách gọi hàm map dùng chung
        return mapToResponse(course);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseResponse> getAllCourses(Pageable pageable){
        Page<Course> courses = courseRepository.findAllByNotDeleted(pageable);
        return courses.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseDetailResponse getCourseById(String courseId){
        Course course = courseRepository.findByIdAndNotDeleted(courseId)
                .orElseThrow(()->new AppException(CourseErrorCode.COURSE_NOT_FOUND));

        // 1. Load danh sách Sections của khóa học, sắp xếp theo orderIndex
        List<Section> sections = sectionRepository.findByCourseIdAndDeletedFalseOrderByOrderIndexAsc(courseId);

        // 2. Map từng Section sang SectionResponse kèm danh sách Lessons bên trong
        List<SectionResponse> sectionResponses = sections.stream().map(section -> {
            List<Lesson> lessons = lessonRepository.findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(section.getId());
            List<LessonResponse> lessonResponses = lessons.stream().map(this::mapToLessonResponse).toList();

            return SectionResponse.builder()
                    .id(section.getId())
                    .courseId(courseId)
                    .title(section.getTitle())
                    .orderIndex(section.getOrderIndex())
                    .lessons(lessonResponses)
                    .build();
        }).toList();

        // 3. Build CourseDetailResponse kèm Curriculum tree
        return CourseDetailResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .slug(course.getSlug())
                .instructorId(course.getInstructorId())
                .categoryId(course.getCategory() != null ? course.getCategory().getId() : null)
                .categoryName(course.getCategory() != null ? course.getCategory().getName() : null)
                .description(course.getDescription())
                .thumbnailUrl(course.getThumbnailUrl())
                .level(course.getLevel())
                .basePrice(course.getBasePrice())
                .currencyCode(course.getCurrencyCode())
                .status(course.getStatus())
                .rejectionNote(course.getRejectionNote())
                .overrideCommissionRate(course.getOverrideCommissionRate())
                .sections(sectionResponses)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseResponse> getAllCoursesByInstructorId(String instructorId, Pageable pageable){
        Page<Course> courses = courseRepository.findByInstructorIdAndNotDeleted(instructorId, pageable);

        return courses.map(this::mapToResponse);

    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseResponse> getAllCoursesForAdmin(Pageable pageable, String status, String instructorId) {
        Page<Course> courses = courseRepository.findAllForAdmin(status, instructorId, pageable);
        return courses.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponse> getRelatedCourses(String courseId) {
        // 1. Lấy thông tin khóa học hiện tại để biết Category
        Course currentCourse = courseRepository.findByIdAndNotDeleted(courseId)
                .orElseThrow(() -> new AppException(CourseErrorCode.COURSE_NOT_FOUND));

        if (currentCourse.getCategory() == null) {
            return List.of();
        }

        // 2. Tìm các khóa học cùng Category (loại trừ khóa học hiện tại)
        // Mặc định lấy 5 khóa học liên quan để gợi ý (Cross-sale)
        Pageable limit = PageRequest.of(0, 5);
        Page<Course> relatedCourses = courseRepository.findRelatedCoursesByCategoryId(
                currentCourse.getCategory().getId(),
                courseId,
                limit
        );

        // 3. Map sang Response
        return relatedCourses.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CourseResponse updateCourse(String courseId, CourseUpdateRequest request, String instructorId){
        Course course = courseRepository.findByIdAndNotDeleted(courseId)
                .orElseThrow(()->new AppException(CourseErrorCode.COURSE_NOT_FOUND));
                
        // 1. Chốt kiểm tra người gọi API có phải chủ sở hữu của khóa học không
        verifyOwnership(course, instructorId);
        
        // 2. Cập nhật từng trường nếu có gửi lên (Partial Update)
        
        if (request.getTitle() != null) {
            course.setTitle(request.getTitle());
        }
        
        if (request.getSlug() != null && !course.getSlug().equals(request.getSlug())) {
            if(courseRepository.existsBySlugAndNotDeleted(request.getSlug())){
                throw new AppException(CourseErrorCode.COURSE_SLUG_EXISTS);
            }
            course.setSlug(request.getSlug());
        }
        
        if (request.getCategoryId() != null && (course.getCategory() == null || !course.getCategory().getId().equals(request.getCategoryId()))) {
            Category category = categoryRepository.findByIdAndDeletedFalse(request.getCategoryId())
                    .orElseThrow(() -> new AppException(CourseErrorCode.CATEGORY_NOT_FOUND));
            course.setCategory(category);
        }
        
        if (request.getDescription() != null) {
            course.setDescription(request.getDescription());
        }
        
        if (request.getLevel() != null) {
            course.setLevel(request.getLevel());
        }
        
        if (request.getBasePrice() != null) {
            course.setBasePrice(request.getBasePrice());
        }
        
        if (request.getCurrencyCode() != null) {
            course.setCurrencyCode(request.getCurrencyCode());
        }
        
        if (request.getThumbnailUrl() != null) {
            course.setThumbnailUrl(request.getThumbnailUrl());
        }

        course = courseRepository.save(course);
        
        // 3. Trả về response
        return mapToResponse(course);
    }

    @Override
    @Transactional
    public CourseResponse updateCourseFull(String courseId, CourseRequest request, String instructorId){
        Course course = courseRepository.findByIdAndNotDeleted(courseId)
                .orElseThrow(()->new AppException(CourseErrorCode.COURSE_NOT_FOUND));
                
        // 1. Chốt kiểm tra người gọi API có phải chủ sở hữu của khóa học không
        verifyOwnership(course, instructorId);
        
        // 2. Nếu có đổi category phải kiểm tra category mới
        if (course.getCategory() == null || !course.getCategory().getId().equals(request.getCategoryId())) {
            Category category = categoryRepository.findByIdAndDeletedFalse(request.getCategoryId())
                    .orElseThrow(() -> new AppException(CourseErrorCode.CATEGORY_NOT_FOUND));
            course.setCategory(category);
        }
        
        // 3. Nếu có đổi Slug , kiểm tra trùng lặp với các course khác
        if(!course.getSlug().equals(request.getSlug())){
            if(courseRepository.existsBySlugAndNotDeleted(request.getSlug())){
                throw new AppException(CourseErrorCode.COURSE_SLUG_EXISTS);
            }
            course.setSlug(request.getSlug());
        }

        // 4. Cập nhật TOÀN BỘ các trường từ request (Replace)
        course.setTitle(request.getTitle());
        course.setDescription(request.getDescription());
        course.setLevel(request.getLevel());
        course.setBasePrice(request.getBasePrice());
        course.setCurrencyCode(request.getCurrencyCode() != null ? request.getCurrencyCode() : "VND");
        course.setThumbnailUrl(request.getThumbnailUrl());

        course = courseRepository.save(course);
        
        // 5. Trả về response
        return mapToResponse(course);
    }

    @Override
    @Transactional
    public void deleteCourse(String courseId, String instructorId){
        Course course = courseRepository.findByIdAndNotDeleted(courseId)
                .orElseThrow(()->new AppException(CourseErrorCode.COURSE_NOT_FOUND));
        //1 chốt kiểm tra người gọi API có phải chủ sở hữu của khóa học không
        verifyOwnership(course, instructorId);
        
        // Cập nhật slug để giải phóng slug cho khóa học mới
        course.setSlug(course.getSlug() + "-deleted-" + System.currentTimeMillis());
        course.setDeleted(true);
        courseRepository.save(course);
    }

    @Override
    @Transactional
    public CourseResponse cloneCourse(String courseId, String instructorId) {
        Course originalCourse = courseRepository.findByIdAndNotDeleted(courseId)
                .orElseThrow(() -> new AppException(CourseErrorCode.COURSE_NOT_FOUND));

        verifyOwnership(originalCourse, instructorId);

        // Tạo course mới (clone) - tạm thời copy các trường cơ bản, phần Section/Lesson có thể clone sau nếu cần
        Course clonedCourse = Course.builder()
                .title(originalCourse.getTitle() + " (Copy)")
                .slug(originalCourse.getSlug() + "-copy-" + System.currentTimeMillis())
                .category(originalCourse.getCategory())
                .instructorId(instructorId)
                .description(originalCourse.getDescription())
                .thumbnailUrl(originalCourse.getThumbnailUrl())
                .level(originalCourse.getLevel())
                .basePrice(originalCourse.getBasePrice())
                .currencyCode(originalCourse.getCurrencyCode())
                .status("DRAFT")
                .overrideCommissionRate(originalCourse.getOverrideCommissionRate())
                .build();

        return mapToResponse(courseRepository.save(clonedCourse));
    }

    @Override
    @Transactional
    public void changeCourseStatus(String courseId, String status, String instructorId) {
        Course course = courseRepository.findByIdAndNotDeleted(courseId)
                .orElseThrow(() -> new AppException(CourseErrorCode.COURSE_NOT_FOUND));

        verifyOwnership(course, instructorId);

        // Chỉ cho phép Giảng viên chuyển sang DRAFT hoặc PENDING (xin duyệt)
        if (!"DRAFT".equals(status) && !"PENDING".equals(status)) {
            throw new AppException(CourseErrorCode.COURSE_INVALID_STATUS);
        }

        course.setStatus(status);
        courseRepository.save(course);
    }

    @Override
    @Transactional
    public void approveCourse(String courseId) {
        Course course = courseRepository.findByIdAndNotDeleted(courseId)
                .orElseThrow(() -> new AppException(CourseErrorCode.COURSE_NOT_FOUND));

        // Nếu đã xuất bản thì ko duyệt lại
        if ("PUBLISHED".equals(course.getStatus())) {
             throw new AppException(CourseErrorCode.COURSE_ALREADY_APPROVED);
        }

        course.setStatus("PUBLISHED");
        course.setRejectionNote(null); // Xóa ghi chú từ chối khi duyệt thành công
        courseRepository.save(course);
    }

    @Override
    @Transactional
    public void rejectCourse(String courseId, String rejectionNote) {
        Course course = courseRepository.findByIdAndNotDeleted(courseId)
                .orElseThrow(() -> new AppException(CourseErrorCode.COURSE_NOT_FOUND));

        if ("REJECTED".equals(course.getStatus())) {
            throw new AppException(CourseErrorCode.COURSE_ALREADY_REJECTED);
        }

        course.setStatus("REJECTED");
        course.setRejectionNote(rejectionNote);
        courseRepository.save(course);
    }

    @Override
    @Transactional
    public void setCourseCommission(String courseId, BigDecimal overrideCommissionRate) {
        Course course = courseRepository.findByIdAndNotDeleted(courseId)
                .orElseThrow(() -> new AppException(CourseErrorCode.COURSE_NOT_FOUND));

        course.setOverrideCommissionRate(overrideCommissionRate);
        courseRepository.save(course);
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
                .rejectionNote(course.getRejectionNote())
                .overrideCommissionRate(course.getOverrideCommissionRate())
                .build();
    }

    // ================= HÀM HỖ TRỢ BẢO MẬT (HELPER) =================

    /**
     * Xác thực quyền sở hữu giữa người gọi API (instructorId) và Data
     */
    private void verifyOwnership(Course course, String instructorId) {
        if (!course.getInstructorId().equals(instructorId)) {
            // Không phải chủ khóa học -> Ném lỗi Unathorized
            throw new AppException(CourseErrorCode.UNAUTHORIZED_ACTION);
        }
    }

    // ================= HÀM MAP LESSON ĐA HÌNH (HELPER) =================

    /**
     * Map Lesson entity (đa hình) sang LessonResponse DTO
     */
    private LessonResponse mapToLessonResponse(Lesson lesson) {
        LessonResponse.LessonResponseBuilder builder = LessonResponse.builder()
                .id(lesson.getId())
                .title(lesson.getTitle())
                .type(lesson.getType())
                .orderIndex(lesson.getOrderIndex())
                .freePreview(lesson.getFreePreview());

        if (lesson instanceof VideoLesson video) {
            builder.videoUrl(video.getVideoUrl());
            builder.duration(video.getDuration());
        } else if (lesson instanceof Quiz quiz) {
            builder.passScore(quiz.getPassScore());
        }

        return builder.build();
    }
}