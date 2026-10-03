package com.lms.course.controller;

import com.lms.common.dto.response.ApiResponse;
import com.lms.course.dto.request.LessonUpdateContentRequest;
import com.lms.course.service.LessonService;
import com.lms.course.service.CourseAccessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/v1/lessons")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Lesson Controller", description = "Quản lý chi tiết một Bài giảng (Video, Quiz)")
public class LessonController {

    LessonService lessonService;
    CourseAccessService courseAccessService;

    @Operation(summary = "27. Cập nhật nội dung Bài học", description = "Cập nhật nội dung Bài học (URL Video hoặc Pass Score)")
    @PutMapping("/{lessonId}/content")
    @PreAuthorize("hasAuthority('COURSE_UPDATE')")
    public ApiResponse<Void> updateLessonContent(
            @PathVariable String lessonId,
            @Valid @RequestBody LessonUpdateContentRequest request,
            Authentication authentication) {
        courseAccessService.requireManageLesson(lessonId, authentication);
        lessonService.updateLessonContent(lessonId, request);
        return ApiResponse.success(null);
    }

    @Operation(summary = "25. Xóa Bài học", description = "Xóa Bài học")
    @DeleteMapping("/{lessonId}")
    @PreAuthorize("hasAuthority('COURSE_UPDATE')")
    public ApiResponse<Void> deleteLesson(@PathVariable String lessonId, Authentication authentication) {
        courseAccessService.requireManageLesson(lessonId, authentication);
        lessonService.deleteLesson(lessonId);
        return ApiResponse.success(null);
    }

    @Operation(summary = "34. Xin cấp URL Xem Video An toàn", description = "Lấy URL xem video an toàn từ MinIO thông qua Media Service")
    @GetMapping("/{lessonId}/play")
    @PreAuthorize("hasAnyAuthority('COURSE_LEARN', 'COURSE_UPDATE', 'COURSE_APPROVE', 'ADMIN')")
    public ApiResponse<String> getPlayUrl(@PathVariable String lessonId, Authentication authentication) {
        courseAccessService.requireLessonAccess(lessonId, authentication);
        String playUrl = lessonService.getPlayUrl(lessonId);
        return ApiResponse.success(playUrl);
    }
}
