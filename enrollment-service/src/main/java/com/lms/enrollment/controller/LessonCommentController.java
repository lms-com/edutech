package com.lms.enrollment.controller;

import com.lms.common.dto.response.ApiResponse;
import com.lms.enrollment.dto.request.CreateCommentRequest;
import com.lms.enrollment.dto.response.LessonCommentResponse;
import com.lms.enrollment.service.LessonCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Lesson Comment Controller", description = "Quản lý bình luận & thảo luận hỏi đáp (Q&A) bài học")
public class LessonCommentController {

    LessonCommentService commentService;

    @Operation(summary = "Lấy danh sách bình luận Q&A của bài học (kèm replies)")
    @GetMapping("/lessons/{lessonId}")
    public ApiResponse<Page<LessonCommentResponse>> getLessonComments(
            @PathVariable String lessonId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(commentService.getCommentsByLesson(lessonId, PageRequest.of(page, size)));
    }

    @Operation(summary = "Gửi câu hỏi hoặc trả lời thảo luận bài học")
    @PostMapping("/courses/{courseId}/lessons/{lessonId}")
    public ApiResponse<LessonCommentResponse> createComment(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String courseId,
            @PathVariable String lessonId,
            @Valid @RequestBody CreateCommentRequest request) {
        return ApiResponse.success(commentService.createComment(courseId, lessonId, request, userId));
    }

    @Operation(summary = "Xóa bình luận của mình")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteComment(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String id) {
        commentService.deleteComment(id, userId);
        return ApiResponse.success(null);
    }

    @Operation(summary = "Thích / Thả tim bình luận")
    @PostMapping("/{id}/like")
    public ApiResponse<LessonCommentResponse> likeComment(@PathVariable String id) {
        return ApiResponse.success(commentService.likeComment(id));
    }
}
