package com.lms.course.controller;

import com.lms.common.dto.response.ApiResponse;
import com.lms.course.dto.request.LessonUpdateContentRequest;
import com.lms.course.service.LessonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/lessons")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Lesson Controller", description = "Quản lý chi tiết một Bài giảng (Video, Quiz)")
public class LessonController {

    LessonService lessonService;

    @Operation(summary = "27. Cập nhật nội dung Bài học", description = "Cập nhật nội dung Bài học (URL Video hoặc Pass Score)")
    @PutMapping("/{lessonId}/content")
    public ApiResponse<Void> updateLessonContent(@PathVariable String lessonId, @Valid @RequestBody LessonUpdateContentRequest request) {
        lessonService.updateLessonContent(lessonId, request);
        return ApiResponse.success(null);
    }

    @Operation(summary = "25. Xóa Bài học", description = "Xóa Bài học")
    @DeleteMapping("/{lessonId}")
    public ApiResponse<Void> deleteLesson(@PathVariable String lessonId) {
        lessonService.deleteLesson(lessonId);
        return ApiResponse.success(null);
    }
}