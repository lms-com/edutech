package com.lms.course.controller;

import com.lms.common.dto.response.ApiResponse;
import com.lms.course.dto.request.CourseRequest;
import com.lms.course.dto.request.CourseStatusUpdateRequest;
import com.lms.course.dto.request.CourseUpdateRequest;
import com.lms.course.dto.request.ReorderRequest;
import com.lms.course.dto.request.SectionCreateRequest;
import com.lms.course.dto.response.CourseResponse;
import com.lms.course.dto.response.SectionResponse;
import com.lms.course.service.CourseService;
import com.lms.course.service.SectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Pageable;
import java.util.List;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
@Tag(name = "Course Controller", description = "Quản lý khóa học dành cho Giảng viên")
public class CourseController {

    private final CourseService courseService;
    private final SectionService sectionService;

    @Operation(
            summary = "12. Tạo khóa học mới (Bản nháp)",
            description = "Tạo bản nháp khóa học mới")
    @PostMapping
//    @PreAuthorize("hasAuthority('COURSE_CREATE')")
    public ApiResponse<CourseResponse> createCourse(@Valid @RequestBody CourseRequest request) {
        // Tạm thời fix cứng instructorId để test CRUD, sau này sẽ lấy từ SecurityContext
        String mockInstructorId = "uuid-instructor-test-123";
        CourseResponse response = courseService.createCourse(request, mockInstructorId);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "8. Lấy chi tiết khóa học",
            description = "Lấy chi tiết khóa học và cấu trúc chương trình học (Curriculum)")
    @GetMapping("/{courseId}")
    public ApiResponse<CourseResponse> getCourseById(@PathVariable String courseId){
        CourseResponse response = courseService.getCourseById(courseId);
        return ApiResponse.success(response);

    }

    @Operation(
            summary = "7. Lấy tất cả các khóa học",
            description = "Tìm kiếm, lọc và phân trang khóa học (Public)")
    @GetMapping
    public ApiResponse<Page<CourseResponse>> getAllCourses(Pageable pageable){
        Page<CourseResponse> response = courseService.getAllCourses(pageable);
        return ApiResponse.success(response);

    }
    
    @Operation(summary = "9. Lấy danh sách khóa học liên quan", description = "Lấy danh sách khóa học liên quan để gợi ý (Cross-sale)")
    @GetMapping("/{courseId}/related")
    public ApiResponse<List<CourseResponse>> getRelatedCourses(@PathVariable String courseId) {
        List<CourseResponse> response = courseService.getRelatedCourses(courseId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "10. Lấy khóa học của tôi", description = "Lấy danh sách khóa học của Giảng viên đang đăng nhập")
    @GetMapping("/my-courses")
//    @PreAuthorize("hasAuthority('COURSE_VIEW_OWN')")
    public ApiResponse<Page<CourseResponse>> getMyCourses(
            // Giả định Gateway parse JWT và truyền userId qua Header. Hoặc lấy từ SecurityContextHolder
            @RequestHeader("X-User-Id") String instructorId,
            Pageable pageable) {
        Page<CourseResponse> response = courseService.getAllCoursesByInstructorId(instructorId, pageable);
        return ApiResponse.success(response);
    }

    @Operation(summary = "13a. Cập nhật khóa học (Từng phần)", description = "Cập nhật một hoặc nhiều thông tin của khóa học. Chỉ cần gửi trường nào muốn sửa.")
    @PatchMapping("/{courseId}")
//    @PreAuthorize("hasAuthority('COURSE_UPDATE')")
    public ApiResponse<CourseResponse> updateCoursePartial(
                @PathVariable String courseId,
                @RequestBody CourseUpdateRequest request,
                @RequestHeader("X-Instructor-Id") String instructorId) {

            CourseResponse courseResponse = courseService.updateCourse(courseId, request, instructorId);
            return ApiResponse.success(courseResponse);

    }
    
    @Operation(summary = "13b. Cập nhật khóa học (Đầy đủ)", description = "Thay thế toàn bộ thông tin của khóa học. Bắt buộc gửi đủ các trường bắt buộc (title, slug, categoryId, basePrice).")
    @PutMapping("/{courseId}")
//    @PreAuthorize("hasAuthority('COURSE_UPDATE')")
    public ApiResponse<CourseResponse> updateCourseFull(
                @PathVariable String courseId,
                @Valid @RequestBody CourseRequest request,
                @RequestHeader("X-Instructor-Id") String instructorId) {

            CourseResponse courseResponse = courseService.updateCourseFull(courseId, request, instructorId);
            return ApiResponse.success(courseResponse);

    }
    
    @Operation(summary = "14. Nhân bản khóa học", description = "Nhân bản khóa học hiện tại thành một bản nháp mới")
    @PostMapping("/{courseId}/clone")
//    @PreAuthorize("hasAuthority('COURSE_CREATE')")
    public ApiResponse<CourseResponse> cloneCourse(
            @PathVariable String courseId,
            @RequestHeader("X-Instructor-Id") String instructorId) {
        CourseResponse response = courseService.cloneCourse(courseId, instructorId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "15. Yêu cầu duyệt / Đổi trạng thái khóa học", description = "Chuyển trạng thái khóa học (ví dụ: DRAFT -> PENDING)")
    @PutMapping("/{courseId}/status")
//    @PreAuthorize("hasAuthority('COURSE_UPDATE')")
    public ApiResponse<Void> changeCourseStatus(
            @PathVariable String courseId,
            @Valid @RequestBody CourseStatusUpdateRequest request,
            @RequestHeader("X-Instructor-Id") String instructorId) {
        courseService.changeCourseStatus(courseId, request.getStatus(), instructorId);
        return ApiResponse.success(null);
    }

    @Operation(
            summary = "19. Gỡ khóa học (Soft Delete / Archive)",
            description = "Gỡ/Xóa mềm khóa học")
    @DeleteMapping("/{courseId}")
//    @PreAuthorize("hasAuthority('COURSE_DELETE')")
    public ApiResponse<Void> deleteCourse(
            @PathVariable String courseId,
            @RequestHeader("X-Instructor-Id") String instructorId) {

        courseService.deleteCourse(courseId, instructorId);
        return ApiResponse.success(null);
    }


    @Operation(summary = "23. Sắp xếp lại thứ tự các Chương", description = "Sắp xếp lại thứ tự các Chương")
    @PutMapping("/{courseId}/sections/reorder")
    public ApiResponse<Void> reorderSections(@PathVariable String courseId, @RequestBody ReorderRequest request) {
        sectionService.reorderSections(courseId, request.getOrderedIds());
        return ApiResponse.success(null);
    }
}