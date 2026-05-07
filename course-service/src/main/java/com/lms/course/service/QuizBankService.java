package com.lms.course.service;

import com.lms.course.dto.request.QuizBankCreateRequest;

public interface QuizBankService {
    Integer createQuizBank(QuizBankCreateRequest request);
}