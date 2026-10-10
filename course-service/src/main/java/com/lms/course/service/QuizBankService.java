package com.lms.course.service;

import com.lms.course.dto.request.QuestionUpdateRequest;
import com.lms.course.dto.request.QuizBankCreateRequest;
import com.lms.course.dto.response.QuestionResponse;

import java.util.List;

public interface QuizBankService {
    Integer createQuizBank(QuizBankCreateRequest request);

    // API 29: Cập nhật 1 câu hỏi và đáp án
    QuestionResponse updateQuestion(String questionId, QuestionUpdateRequest request);

    // API 30: Xóa 1 câu hỏi
    void deleteQuestion(String questionId);

    // API 31: Sắp xếp lại thứ tự câu hỏi
    void reorderQuestions(String lessonId, List<String> orderedIds);

    // API 32: Lấy danh sách câu hỏi (có tùy chọn ẩn đáp án)
    List<QuestionResponse> getQuestionsByLessonId(String lessonId, boolean hideCorrectAnswer);
}