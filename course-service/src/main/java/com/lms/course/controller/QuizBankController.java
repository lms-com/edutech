package com.lms.course.controller;

import com.lms.common.dto.response.ApiResponse;
import com.lms.course.dto.request.QuestionUpdateRequest;
import com.lms.course.dto.request.QuizBankCreateRequest;
import com.lms.course.dto.request.ReorderRequest;
import com.lms.course.dto.response.QuestionResponse;
import com.lms.course.service.QuizBankService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Quiz Bank Controller", description = "Quản lý Ngân hàng câu hỏi trắc nghiệm")
public class QuizBankController {

    QuizBankService quizBankService;

    @Operation(summary = "28. Tạo câu hỏi hàng loạt", description = "Lưu 1 lần nhiều câu hỏi và đáp án cho một Bài kiểm tra.")
    @PostMapping("/lessons/{lessonId}/questions/bulk")
    public ApiResponse<String> createBulkQuestions(@PathVariable String lessonId, @RequestBody QuizBankCreateRequest request) {
        request.setQuizId(lessonId);
        Integer totalSaved = quizBankService.createQuizBank(request);
        return ApiResponse.success("Đã lưu thành công " + totalSaved + " câu hỏi vào ngân hàng.");
    }

    @Operation(summary = "29. Cập nhật 1 câu hỏi", description = "Cập nhật nội dung câu hỏi và danh sách đáp án đi kèm.")
    @PutMapping("/questions/{questionId}")
    public ApiResponse<QuestionResponse> updateQuestion(
            @PathVariable String questionId,
            @Valid @RequestBody QuestionUpdateRequest request) {
        QuestionResponse response = quizBankService.updateQuestion(questionId, request);
        return ApiResponse.success(response);
    }

    @Operation(summary = "30. Xóa 1 câu hỏi", description = "Xóa mềm câu hỏi và các đáp án liên quan.")
    @DeleteMapping("/questions/{questionId}")
    public ApiResponse<Void> deleteQuestion(@PathVariable String questionId) {
        quizBankService.deleteQuestion(questionId);
        return ApiResponse.success(null);
    }

    @Operation(summary = "31. Sắp xếp lại thứ tự câu hỏi", description = "Thay đổi thứ tự hiển thị của các câu hỏi trong bài Quiz.")
    @PutMapping("/lessons/{lessonId}/questions/reorder")
    public ApiResponse<Void> reorderQuestions(
            @PathVariable String lessonId,
            @RequestBody ReorderRequest request) {
        quizBankService.reorderQuestions(lessonId, request.getOrderedIds());
        return ApiResponse.success(null);
    }

    @Operation(summary = "32. Lấy danh sách câu hỏi cho Học viên", description = "Lấy toàn bộ câu hỏi của bài Quiz. (Trường is_correct sẽ bị ẩn để chống gian lận)")
    @GetMapping("/lessons/{lessonId}/questions")
    public ApiResponse<List<QuestionResponse>> getQuestionsForLearner(
            @PathVariable String lessonId,
            @RequestParam(required = false, defaultValue = "true") boolean isLearner) {
        // Nếu là Học viên (isLearner = true), hideCorrectAnswer sẽ là true (Ẩn đáp án đúng)
        List<QuestionResponse> response = quizBankService.getQuestionsByLessonId(lessonId, isLearner);
        return ApiResponse.success(response);
    }
}