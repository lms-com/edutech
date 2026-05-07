package com.lms.course.service;

import com.lms.course.dto.request.LessonCreateRequest;
import com.lms.course.dto.request.LessonUpdateContentRequest;
import com.lms.course.dto.response.LessonResponse;

import java.util.List;

public interface LessonService {
    LessonResponse createLesson(LessonCreateRequest request);

    void updateLessonContent(String lessonId, LessonUpdateContentRequest request);

    void deleteLesson(String lessonId);

    void reorderLessons(String sectionId, List<String> orderedIds);

}