package com.lms.course.service.impl;

import com.lms.common.exception.AppException;
import com.lms.course.dto.request.AnswerItemRequest;
import com.lms.course.dto.request.QuestionItemRequest;
import com.lms.course.dto.request.QuestionUpdateRequest;
import com.lms.course.dto.request.QuizBankCreateRequest;
import com.lms.course.dto.response.AnswerResponse;
import com.lms.course.dto.response.QuestionResponse;
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
import java.util.stream.Collectors;

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

    @Override
    @Transactional
    public QuestionResponse updateQuestion(String questionId, QuestionUpdateRequest request) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new AppException(CourseErrorCode.QUESTION_NOT_FOUND));

        question.setQuestionText(request.getQuestionText());
        questionRepository.save(question);

        // Đơn giản hóa: Xóa mềm tất cả đáp án cũ và thêm đáp án mới
        List<Answer> oldAnswers = answerRepository.findByQuestionIdAndDeletedFalse(questionId);
        oldAnswers.forEach(a -> a.setDeleted(true));
        answerRepository.saveAll(oldAnswers);

        List<Answer> newAnswersToSave = new ArrayList<>();
        boolean hasCorrectAnswer = false;

        if (request.getAnswers() != null) {
            for (AnswerItemRequest item : request.getAnswers()) {
                Answer answer = new Answer();
                answer.setQuestion(question);
                answer.setOptionText(item.getOptionText());
                answer.setCorrect(item.getCorrect() != null ? item.getCorrect() : false);
                newAnswersToSave.add(answer);
                
                if (Boolean.TRUE.equals(answer.getCorrect())) {
                    hasCorrectAnswer = true;
                }
            }
        }

        if (!hasCorrectAnswer && !newAnswersToSave.isEmpty()) {
             throw new AppException(CourseErrorCode.QUESTION_NO_CORRECT_ANSWER);
        }

        answerRepository.saveAll(newAnswersToSave);

        return mapToResponse(question, newAnswersToSave, false);
    }

    @Override
    @Transactional
    public void deleteQuestion(String questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new AppException(CourseErrorCode.QUESTION_NOT_FOUND));
        
        question.setDeleted(true);
        questionRepository.save(question);
        
        List<Answer> answers = answerRepository.findByQuestionIdAndDeletedFalse(questionId);
        answers.forEach(a -> a.setDeleted(true));
        answerRepository.saveAll(answers);
    }

    @Override
    @Transactional
    public void reorderQuestions(String lessonId, List<String> orderedIds) {
        quizRepository.findById(lessonId)
                .orElseThrow(() -> new AppException(CourseErrorCode.LESSON_NOT_FOUND));

        if (orderedIds == null || orderedIds.isEmpty()) return;

        List<Question> questions = questionRepository.findAllById(orderedIds);
        
        for (int i = 0; i < orderedIds.size(); i++) {
            String id = orderedIds.get(i);
            Question question = questions.stream()
                .filter(q -> q.getId().equals(id))
                .findFirst()
                .orElse(null);
                
            if (question != null && question.getQuiz().getId().equals(lessonId)) {
                question.setOrderIndex(i);
            }
        }
        
        questionRepository.saveAll(questions);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionResponse> getQuestionsByLessonId(String lessonId, boolean hideCorrectAnswer) {
        quizRepository.findById(lessonId)
                .orElseThrow(() -> new AppException(CourseErrorCode.LESSON_NOT_FOUND));

        List<Question> questions = questionRepository.findByQuizIdAndNotDeletedOrderByOrderIndexAsc(lessonId);
        
        return questions.stream().map(question -> {
            List<Answer> answers = answerRepository.findByQuestionIdAndDeletedFalse(question.getId());
            return mapToResponse(question, answers, hideCorrectAnswer);
        }).collect(Collectors.toList());
    }

    private QuestionResponse mapToResponse(Question question, List<Answer> answers, boolean hideCorrectAnswer) {
        List<AnswerResponse> answerResponses = answers.stream().map(a -> 
            AnswerResponse.builder()
                .id(a.getId())
                .optionText(a.getOptionText())
                .isCorrect(hideCorrectAnswer ? null : a.getCorrect())
                .build()
        ).collect(Collectors.toList());

        return QuestionResponse.builder()
                .id(question.getId())
                .questionText(question.getQuestionText())
                .orderIndex(question.getOrderIndex())
                .answers(answerResponses)
                .build();
    }
}