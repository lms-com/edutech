package com.lms.course.controller;

import com.lms.common.dto.response.ApiResponse;
import com.lms.course.dto.request.CourseCommissionRequest;
import com.lms.course.dto.request.CourseRejectRequest;
import com.lms.course.dto.response.CourseResponse;
import com.lms.course.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/courses")
@RequiredArgsConstructor
@Tag(name = "Admin Course Controller", description = "Quản lý khóa học dành cho Admin")
public class AdminCourseController {

    private final CourseService courseService;

    @Operation(summary = "11. Lấy toàn bộ khóa học (Admin)", description = "Lấy danh sách toàn bộ khóa học trên hệ thống, hỗ trợ lọc theo status, instructorId và phân trang.")
    @GetMapping
    // @PreAuthorize("hasAuthority('COURSE_VIEW_ALL')") // Nên có để phân quyền
    public ApiResponse<Page<CourseResponse>> getAllCoursesForAdmin(
            Pageable pageable,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String instructorId) {
        Page<CourseResponse> response = courseService.getAllCoursesForAdmin(pageable, status, instructorId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "16. Admin duyệt khóa học", description = "Đổi trạng thái khóa học sang PUBLISHED")
    @PutMapping("/{courseId}/approve")
    // @PreAuthorize("hasAuthority('COURSE_APPROVE')")
    public ApiResponse<Void> approveCourse(@PathVariable String courseId) {
        courseService.approveCourse(courseId);
        return ApiResponse.success(null);
    }

    @Operation(summary = "17. Admin từ chối khóa học", description = "Đổi trạng thái khóa học sang REJECTED kèm lý do")
    @PutMapping("/{courseId}/reject")
    // @PreAuthorize("hasAuthority('COURSE_APPROVE')")
    public ApiResponse<Void> rejectCourse(
            @PathVariable String courseId,
            @Valid @RequestBody CourseRejectRequest request) {
        courseService.rejectCourse(courseId, request.getRejectionNote());
        return ApiResponse.success(null);
    }

    @Operation(summary = "18. Cấu hình tỷ lệ hoa hồng", description = "Admin thiết lập tỷ lệ hoa hồng riêng (override_commission_rate) cho khóa học này")
    @PutMapping("/{courseId}/commission")
    // @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ApiResponse<Void> setCourseCommission(
            @PathVariable String courseId,
            @Valid @RequestBody CourseCommissionRequest request) {
        courseService.setCourseCommission(courseId, request.getOverrideCommissionRate());
        return ApiResponse.success(null);
    }
}