package com.lms.course.controller;

import com.lms.common.dto.response.ApiResponse;
import com.lms.course.dto.request.LessonCreateRequest;
import com.lms.course.dto.request.ReorderRequest;
import com.lms.course.dto.request.SectionCreateRequest;
import com.lms.course.dto.request.SectionUpdateRequest;
import com.lms.course.dto.response.LessonResponse;
import com.lms.course.dto.response.SectionResponse;
import com.lms.course.service.LessonService;
import com.lms.course.service.SectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sections")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Section & Lesson Controller", description = "Quản lý chương học và các bài học bên trong")
public class SectionController {

    SectionService sectionService;
    LessonService lessonService;

    @Operation(summary = "20. Thêm Chương (Section) mới", description = "Thêm Chương (Section) mới")
    @PostMapping
    public ApiResponse<SectionResponse> createSection(@Valid @RequestBody SectionCreateRequest request) {
        SectionResponse response = sectionService.createSection(request);
        return ApiResponse.success(response);
    }


    @Operation(summary = "24. Thêm Bài học", description = "Thêm Bài học (Lesson Đa hình VIDEO/QUIZ)")
    @PostMapping("/{sectionId}/lessons")
    public ApiResponse<LessonResponse> createLesson(@PathVariable String sectionId, @Valid @RequestBody LessonCreateRequest request) {
        // Gán sectionId từ path vào request DTO để service xử lý
        request.setSectionId(sectionId);
        LessonResponse data = lessonService.createLesson(request);
        return ApiResponse.success(data);
    }

    @Operation(summary = "21. Cập nhật tên Chương", description = "Cập nhật tên Chương")
    @PutMapping("/{sectionId}")
    public ApiResponse<Void> updateSection(@PathVariable String sectionId, @Valid @RequestBody SectionUpdateRequest request) {
        sectionService.updateSection(sectionId, request);
        return ApiResponse.success(null);
    }

    @Operation(summary = "22. Xóa Chương", description = "Xóa Chương")
    @DeleteMapping("/{sectionId}")
    public ApiResponse<Void> deleteSection(@PathVariable String sectionId) {
        sectionService.deleteSection(sectionId);
        return ApiResponse.success(null);
    }

    @Operation(summary = "26. Sắp xếp lại thứ tự Bài học", description = "Sắp xếp lại thứ tự Bài học")
    @PutMapping("/{sectionId}/lessons/reorder")
    public ApiResponse<Void> reorderLessons(@PathVariable String sectionId, @RequestBody ReorderRequest request) {
        lessonService.reorderLessons(sectionId, request.getOrderedIds());
        return ApiResponse.success(null);
    }
}