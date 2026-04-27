package com.lms.course.controller;

import com.lms.common.dto.response.ApiResponse;
import com.lms.course.dto.request.LessonCreateRequest;
import com.lms.course.dto.response.LessonResponse;
import com.lms.course.service.LessonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/lessons")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Lesson Controller", description = "Quản lý Bài giảng (Video, Quiz, Doc)")
public class LessonController {

    LessonService lessonService;

    @Operation(summary = "Tạo mới bài giảng", description = "Tạo một bài giảng mới. Hệ thống tự động phân loại lưu thành Video hoặc Quiz dựa vào trường type.")
    @PostMapping
    public ApiResponse<LessonResponse> createLesson(@RequestBody LessonCreateRequest request) {
        LessonResponse data = lessonService.createLesson(request);
        return ApiResponse.success(data);
    }
}