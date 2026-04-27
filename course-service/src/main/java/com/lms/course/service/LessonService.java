package com.lms.course.service;

import com.lms.course.dto.request.LessonCreateRequest;
import com.lms.course.dto.response.LessonResponse;

public interface LessonService {
    LessonResponse createLesson(LessonCreateRequest request);
}