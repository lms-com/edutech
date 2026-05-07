package com.lms.course.service.impl;

import com.lms.common.exception.AppException;
import com.lms.course.dto.request.AnswerItemRequest;
import com.lms.course.dto.request.QuestionItemRequest;
import com.lms.course.dto.request.QuizBankCreateRequest;
import com.lms.course.entity.Answer;
import com.lms.course.entity.Question;
import com.lms.course.entity.Quiz;
import com.lms.course.exception.CourseErrorCode;
import com.lms.course.repository.AnswerRepository;
import com.lms.course.repository.QuestionRepository;
import com.lms.course.repository.QuizRepository;
import com.lms.course.service.QuizBankService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class QuizBankServiceImpl implements QuizBankService {

    QuizRepository quizRepository;
    QuestionRepository questionRepository;
    AnswerRepository answerRepository;

    @Override
    @Transactional // Đảm bảo tính toàn vẹn: Lỗi 1 câu là rollback (hoàn tác) toàn bộ
    public Integer createQuizBank(QuizBankCreateRequest request) {
        // 1. Kiểm tra xem Bài Quiz có tồn tại không
        Quiz quiz = quizRepository.findById(request.getQuizId())
                .orElseThrow(() -> new AppException(CourseErrorCode.LESSON_NOT_FOUND));

        // 2. Tạo 2 rổ rỗng để chứa câu hỏi và đáp án chờ lưu
        List<Question> questionsToSave = new ArrayList<>();
        List<Answer> answersToSave = new ArrayList<>();

        // 3. Duyệt qua cục data to bự mà Frontend gửi lên
        for (QuestionItemRequest qItem : request.getQuestions()) {
            // Tạo thực thể Câu hỏi
            Question question = new Question();
            question.setQuiz(quiz);
            question.setQuestionText(qItem.getQuestionText());
            question.setOrderIndex(qItem.getOrderIndex());
            questionsToSave.add(question); // Quăng vào rổ

            // Duyệt tiếp danh sách đáp án của Câu hỏi này
            if (qItem.getAnswers() != null) {
                for (AnswerItemRequest aItem : qItem.getAnswers()) {
                    // Tạo thực thể Đáp án
                    Answer answer = new Answer();
                    answer.setQuestion(question); // Nối với Câu hỏi vừa tạo
                    answer.setOptionText(aItem.getOptionText());
                    answer.setCorrect(aItem.getCorrect() != null ? aItem.getCorrect() : false);
                    answersToSave.add(answer); // Quăng vào rổ
                }
            }
        }

        // 4. Gọi hàm Bulk Insert (Lưu tất cả cùng một lúc)
        questionRepository.saveAll(questionsToSave);
        answerRepository.saveAll(answersToSave);

        // Trả về tổng số câu hỏi đã được lưu thành công
        return questionsToSave.size();
    }
}