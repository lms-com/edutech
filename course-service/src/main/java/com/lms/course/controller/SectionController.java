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
@RequestMapping("/api/v1/sections")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Section & Lesson Controller", description = "Quản lý chương học và các bài học bên trong")
public class SectionController {

    SectionService sectionService;
    LessonService lessonService;
    CourseAccessService courseAccessService;

    @Operation(summary = "20. Thêm Chương (Section) mới", description = "Thêm Chương (Section) mới")
    @PostMapping
    @PreAuthorize("hasAuthority('COURSE_UPDATE')")
    public ApiResponse<SectionResponse> createSection(
            @Valid @RequestBody SectionCreateRequest request,
            Authentication authentication) {
        courseAccessService.requireManageCourse(request.getCourseId(), authentication);
        SectionResponse response = sectionService.createSection(request);
        return ApiResponse.success(response);
    }


    @Operation(summary = "24. Thêm Bài học", description = "Thêm Bài học (Lesson Đa hình VIDEO/QUIZ)")
    @PostMapping("/{sectionId}/lessons")
    @PreAuthorize("hasAuthority('COURSE_UPDATE')")
    public ApiResponse<LessonResponse> createLesson(
            @PathVariable String sectionId,
            @Valid @RequestBody LessonCreateRequest request,
            Authentication authentication) {
        courseAccessService.requireManageSection(sectionId, authentication);
        // Gán sectionId từ path vào request DTO để service xử lý
        request.setSectionId(sectionId);
        LessonResponse data = lessonService.createLesson(request);
        return ApiResponse.success(data);
    }

    @Operation(summary = "21. Cập nhật tên Chương", description = "Cập nhật tên Chương")
    @PutMapping("/{sectionId}")
    @PreAuthorize("hasAuthority('COURSE_UPDATE')")
    public ApiResponse<Void> updateSection(
            @PathVariable String sectionId,
            @Valid @RequestBody SectionUpdateRequest request,
            Authentication authentication) {
        courseAccessService.requireManageSection(sectionId, authentication);
        sectionService.updateSection(sectionId, request);
        return ApiResponse.success(null);
    }

    @Operation(summary = "22. Xóa Chương", description = "Xóa Chương")
    @DeleteMapping("/{sectionId}")
    @PreAuthorize("hasAuthority('COURSE_UPDATE')")
    public ApiResponse<Void> deleteSection(@PathVariable String sectionId, Authentication authentication) {
        courseAccessService.requireManageSection(sectionId, authentication);
        sectionService.deleteSection(sectionId);
        return ApiResponse.success(null);
    }

    @Operation(summary = "26. Sắp xếp lại thứ tự Bài học", description = "Sắp xếp lại thứ tự Bài học")
    @PutMapping("/{sectionId}/lessons/reorder")
    @PreAuthorize("hasAuthority('COURSE_UPDATE')")
    public ApiResponse<Void> reorderLessons(
            @PathVariable String sectionId,
            @RequestBody ReorderRequest request,
            Authentication authentication) {
        courseAccessService.requireManageSection(sectionId, authentication);
        lessonService.reorderLessons(sectionId, request.getOrderedIds());
        return ApiResponse.success(null);
    }
}
