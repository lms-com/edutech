package com.lms.course.controller;

import com.lms.common.dto.response.ApiResponse;
import com.lms.course.dto.request.LessonAttachmentRequest;
import com.lms.course.dto.response.LessonAttachmentResponse;
import com.lms.course.service.CourseAccessService;
import com.lms.course.service.LessonAttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/lessons")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Lesson Attachment Controller", description = "Quản lý tài liệu đính kèm cho bài học")
public class LessonAttachmentController {

    LessonAttachmentService attachmentService;
    CourseAccessService courseAccessService;

    @Operation(summary = "Lấy danh sách tài liệu đính kèm của bài học", description = "Học viên hoặc Giảng viên lấy danh sách tài liệu bài học để tải về")
    @GetMapping("/{lessonId}/attachments")
    @PreAuthorize("hasAnyAuthority('COURSE_LEARN', 'COURSE_UPDATE', 'COURSE_APPROVE', 'ADMIN')")
    public ApiResponse<List<LessonAttachmentResponse>> getLessonAttachments(
            @PathVariable String lessonId,
            Authentication authentication) {
        courseAccessService.requireLessonAccess(lessonId, authentication);
        List<LessonAttachmentResponse> attachments = attachmentService.getAttachments(lessonId);
        return ApiResponse.success(attachments);
    }

    @Operation(summary = "Thêm tài liệu đính kèm vào bài học", description = "Giảng viên thêm tài liệu đính kèm (PDF, Slide, ZIP) vào bài học")
    @PostMapping("/{lessonId}/attachments")
    @PreAuthorize("hasAuthority('COURSE_UPDATE')")
    public ApiResponse<LessonAttachmentResponse> addAttachment(
            @PathVariable String lessonId,
            @Valid @RequestBody LessonAttachmentRequest request,
            Authentication authentication) {
        courseAccessService.requireManageLesson(lessonId, authentication);
        LessonAttachmentResponse response = attachmentService.addAttachment(lessonId, request);
        return ApiResponse.success(response);
    }

    @Operation(summary = "Xóa tài liệu đính kèm", description = "Giảng viên xóa tài liệu đính kèm của bài học")
    @DeleteMapping("/attachments/{attachmentId}")
    @PreAuthorize("hasAuthority('COURSE_UPDATE')")
    public ApiResponse<Void> deleteAttachment(
            @PathVariable String attachmentId,
            Authentication authentication) {
        attachmentService.deleteAttachment(attachmentId);
        return ApiResponse.success(null);
    }
}
