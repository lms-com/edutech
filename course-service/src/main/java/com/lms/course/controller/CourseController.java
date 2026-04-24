package com.lms.course.controller;

import com.lms.common.dto.response.ApiResponse;
import com.lms.course.dto.request.CourseRequest;
import com.lms.course.dto.response.CourseResponse;
import com.lms.course.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Pageable;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
@Tag(name = "Course Controller", description = "Quản lý khóa học dành cho Giảng viên")
public class CourseController {

    private final CourseService courseService;

    @Operation(
            summary = "Tạo khóa học mới (Bản nháp)",
            description = "Giảng viên tạo khung khóa học ban đầu")
    @PostMapping
    @PreAuthorize("hasAuthority('COURSE_CREATE')")
    public ApiResponse<CourseResponse> createCourse(@Valid @RequestBody CourseRequest request) {
        // Tạm thời fix cứng instructorId để test CRUD, sau này sẽ lấy từ SecurityContext
        String mockInstructorId = "uuid-instructor-test-123";
        CourseResponse response = courseService.createCourse(request, mockInstructorId);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "Lấy chi tiết khóa học ",
            description = "Lấy chi tiết khóa học theo ID")
    @GetMapping("/{courseId}")
    public ApiResponse<CourseResponse> getCourseById(@PathVariable String courseId){
        CourseResponse response = courseService.getCourseById(courseId);
        return ApiResponse.success(response);

    }

    @Operation(
            summary = "Lấy tất cả các khóa học",
            description = "Lấy danh sách tất cả các khóa học trả về kiểu phân trang ")
    @GetMapping
    public ApiResponse<Page<CourseResponse>> getAllCourses(Pageable pageable){
        Page<CourseResponse> response = courseService.getAllCourses(pageable);
        return ApiResponse.success(response);

    }
    @Operation(summary = "Lấy khóa học của tôi", description = "Lấy danh sách khóa học do giảng viên đang đăng nhập tạo ra")
    @GetMapping("/my-courses")
    @PreAuthorize("hasAuthority('COURSE_VIEW_OWN')")
    public ApiResponse<Page<CourseResponse>> getMyCourses(
            // Giả định Gateway parse JWT và truyền userId qua Header. Hoặc lấy từ SecurityContextHolder
            @RequestHeader("X-User-Id") String instructorId,
            Pageable pageable) {
        Page<CourseResponse> response = courseService.getAllCoursesByInstructorId(instructorId, pageable);
        return ApiResponse.success(response);
    }

    @Operation(summary = "Cập nhật khóa học", description = "Cập nhật thông tin khóa học theo ID")
    @PutMapping("/{courseId}")
    @PreAuthorize("hasAuthority('COURSE_UPDATE')")
    public ApiResponse<CourseResponse> updateCourse(
                @PathVariable String courseId,
                @Valid @RequestBody CourseRequest request,
                @RequestHeader("X-Instructor-Id") String instructorId) {

            CourseResponse courseResponse = courseService.updateCourse(courseId, request, instructorId);
            return ApiResponse.success(courseResponse);

    }

    @Operation(
            summary = "xóa mềm khóa học ",
            description = "Giảng viên xóa khóa học (is_Deleted = true)")
    @DeleteMapping
    @PreAuthorize("hasAuthority('COURSE_DELETE')")
    public ApiResponse<Void> deleteCourse(
            @PathVariable String courseId,
            @RequestHeader("X-Instructor-Id") String instructorId) {

        courseService.deleteCourse(courseId, instructorId);
        return ApiResponse.success(null);
    }




}