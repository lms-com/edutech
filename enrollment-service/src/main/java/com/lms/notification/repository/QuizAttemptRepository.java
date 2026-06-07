package com.lms.notification.repository;

import com.lms.notification.entity.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, String> {
    List<QuizAttempt> findAllByEnrollmentId(String enrollmentId);
    List<QuizAttempt> findAllByEnrollmentIdAndLessonId(String enrollmentId, String lessonId);
}
