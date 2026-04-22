package com.lms.course.controller;

import com.lms.common.dto.response.ApiResponse;
import com.lms.course.dto.request.CourseRequest;
import com.lms.course.dto.response.CourseResponse;
import com.lms.course.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
@Tag(name = "Course Controller", description = "Quản lý khóa học dành cho Giảng viên")
public class CourseController {

    private final CourseService courseService;

    @Operation(summary = "Tạo khóa học mới (Bản nháp)", description = "Giảng viên tạo khung khóa học ban đầu")
    @PostMapping
    @PreAuthorize("hasAuthority('COURSE_CREATE')")
    public ApiResponse<CourseResponse> createCourse(@Valid @RequestBody CourseRequest request) {
        // Tạm thời fix cứng instructorId để test CRUD, sau này sẽ lấy từ SecurityContext
        String mockInstructorId = "uuid-instructor-test-123";
        CourseResponse response = courseService.createCourse(request, mockInstructorId);
        return ApiResponse.success(response);
    }
}