package com.lms.course.controller;

import com.lms.common.dto.response.ApiResponse;
import com.lms.course.dto.response.*;
import com.lms.course.service.InternalCourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller cho các API nội bộ (Internal APIs).
 * Chỉ được gọi bởi các Microservice khác trong cùng VPC thông qua Feign Client / RestTemplate.
 * KHÔNG được expose ra ngoài qua API Gateway.
 */
@RestController
@RequestMapping("/api/internal/v1")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Internal Course API", description = "API nội bộ - Chỉ dành cho giao tiếp giữa các Microservice")
public class InternalCourseController {

    InternalCourseService internalCourseService;

    @Operation(summary = "35. Lấy thông tin giá khóa học",
               description = "Order Service gọi để lấy giá chính xác khi tạo đơn hàng.")
    @GetMapping("/courses/{courseId}/price-info")
    public ApiResponse<CoursePriceResponse> getCoursePriceInfo(@PathVariable String courseId) {
        return ApiResponse.success(internalCourseService.getCoursePriceInfo(courseId));
    }

    @Operation(summary = "36. Lấy thông tin cơ bản nhiều khóa học",
               description = "Enrollment/Dashboard Service gọi để hiển thị danh sách khóa học đã đăng ký.")
    @PostMapping("/courses/batch")
    public ApiResponse<List<CourseBatchResponse>> getCourseBatch(@RequestBody List<String> courseIds) {
        return ApiResponse.success(internalCourseService.getCourseBatch(courseIds));
    }

    @Operation(summary = "37. Đếm tổng bài học của khóa học",
               description = "Enrollment Service gọi để tính phần trăm tiến độ học tập.")
    @GetMapping("/courses/{courseId}/lesson-count")
    public ApiResponse<LessonCountResponse> getLessonCount(@PathVariable String courseId) {
        return ApiResponse.success(internalCourseService.getLessonCount(courseId));
    }

    @Operation(summary = "38. Xác minh tính hợp lệ của bài học",
               description = "Enrollment Service gọi trước khi ghi nhận hoàn thành bài học.")
    @GetMapping("/lessons/{lessonId}/validation")
    public ApiResponse<LessonValidationResponse> validateLesson(@PathVariable String lessonId) {
        return ApiResponse.success(internalCourseService.validateLesson(lessonId));
    }

    @Operation(summary = "39. Lấy danh sách đáp án đúng",
               description = "Enrollment Service gọi để chấm điểm bài Quiz tự động.")
    @GetMapping("/lessons/{lessonId}/correct-answers")
    public ApiResponse<List<CorrectAnswerResponse>> getCorrectAnswers(@PathVariable String lessonId) {
        return ApiResponse.success(internalCourseService.getCorrectAnswers(lessonId));
    }

    @Operation(summary = "40. Lấy thông tin cơ bản + giá nhiều khóa học (Bulk)",
               description = "Finance Service gọi khi nhận event order.completed để lấy basePrice + instructorId phục vụ Revenue Split.")
    @PostMapping("/courses/bulk")
    public ApiResponse<List<CourseBulkResponse>> getCourseBulk(@RequestBody List<String> courseIds) {
        return ApiResponse.success(internalCourseService.getCourseBulk(courseIds));
    }
}
