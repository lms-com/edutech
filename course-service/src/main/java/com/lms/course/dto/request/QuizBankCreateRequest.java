package com.lms.course.dto.request;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import java.util.List;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuizBankCreateRequest {
    String quizId; // ID của Lesson (dạng Quiz) đã tạo trước đó
    List<QuestionItemRequest> questions;
}