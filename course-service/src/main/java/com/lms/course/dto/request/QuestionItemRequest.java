package com.lms.course.dto.request;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import java.util.List;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuestionItemRequest {
    String questionText;
    Integer orderIndex;
    List<AnswerItemRequest> answers; // Chứa danh sách các đáp án (thường là 4 đáp án A,B,C,D)
}