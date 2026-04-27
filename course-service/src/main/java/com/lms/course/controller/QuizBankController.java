package com.lms.course.controller;

import com.lms.common.dto.response.ApiResponse;
import com.lms.course.dto.request.QuizBankCreateRequest;
import com.lms.course.service.QuizBankService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/quiz-bank")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Quiz Bank Controller", description = "Quản lý Ngân hàng câu hỏi trắc nghiệm")
public class QuizBankController {

    QuizBankService quizBankService;

    @Operation(summary = "Tạo câu hỏi hàng loạt", description = "Lưu 1 lần nhiều câu hỏi và đáp án cho một Bài kiểm tra.")
    @PostMapping("/bulk")
    public ApiResponse<String> createBulkQuestions(@RequestBody QuizBankCreateRequest request) {
        Integer totalSaved = quizBankService.createQuizBank(request);
        return ApiResponse.success("Đã lưu thành công " + totalSaved + " câu hỏi vào ngân hàng.");
    }
}