package com.lms.enrollment.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Yêu cầu nộp bài kiểm tra.
 *
 * Client gửi LỰA CHỌN của mình, không gửi điểm. Trước đây DTO này chỉ có `score`
 * do client tự tính, nên bất kỳ ai cũng nộp được điểm tuyệt đối mà không cần làm
 * bài — đồng thời việc backend ẩn đáp án đúng khỏi API học viên trở nên vô nghĩa
 * vì điểm số đã do phía client quyết định.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizSubmitRequest {

    @NotEmpty(message = "Quiz answers must not be empty")
    @Valid
    private List<QuizAnswerRequest> answers;
}
