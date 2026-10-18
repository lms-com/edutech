package com.lms.enrollment.service;

import com.lms.enrollment.dto.request.CreateCommentRequest;
import com.lms.enrollment.dto.response.LessonCommentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LessonCommentService {

    /**
     * Lấy danh sách bình luận (Q&A) của một bài học, gom kèm các câu trả lời (replies).
     */
    Page<LessonCommentResponse> getCommentsByLesson(String lessonId, Pageable pageable);

    /**
     * Thêm bình luận hoặc câu hỏi mới cho bài học.
     */
    LessonCommentResponse createComment(String courseId, String lessonId, CreateCommentRequest request, String userId);

    /**
     * Xóa bình luận (chỉ chính chủ hoặc Admin được xóa).
     */
    void deleteComment(String commentId, String userId);

    /**
     * Thích / Thả tim bình luận.
     */
    LessonCommentResponse likeComment(String commentId);
}
