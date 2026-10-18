package com.lms.enrollment.repository;

import com.lms.enrollment.entity.LessonComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonCommentRepository extends JpaRepository<LessonComment, String> {

    // Lấy các bình luận gốc (top-level, parentId is null) của một bài học, sắp xếp theo thời gian mới nhất
    Page<LessonComment> findByLessonIdAndParentIdIsNullOrderByCreatedAtDesc(String lessonId, Pageable pageable);

    // Lấy các câu trả lời (replies) theo parentId
    List<LessonComment> findByParentIdOrderByCreatedAtAsc(String parentId);

    // Lấy toàn bộ bình luận trong một bài học (nếu cần gom nhóm)
    List<LessonComment> findByLessonIdOrderByCreatedAtAsc(String lessonId);

    // Đếm tổng số bình luận của bài học
    long countByLessonId(String lessonId);
}
