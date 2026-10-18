package com.lms.course.repository;

import com.lms.course.entity.LessonAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonAttachmentRepository extends JpaRepository<LessonAttachment, String> {

    List<LessonAttachment> findByLessonIdOrderByCreatedAtAsc(String lessonId);

    void deleteByLessonId(String lessonId);
}
