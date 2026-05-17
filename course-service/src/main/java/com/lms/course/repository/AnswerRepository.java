package com.lms.course.repository;

import com.lms.course.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, String> {
    List<Answer> findByQuestionIdAndDeletedFalse(String questionId);

    // Internal API 39: Lấy danh sách đáp án đúng cho chấm điểm tự động
    List<Answer> findByQuestionIdAndDeletedFalseAndCorrectTrue(String questionId);
}