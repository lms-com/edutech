package com.lms.course.service;

import com.lms.course.dto.request.LessonAttachmentRequest;
import com.lms.course.dto.response.LessonAttachmentResponse;

import java.util.List;

public interface LessonAttachmentService {

    List<LessonAttachmentResponse> getAttachments(String lessonId);

    LessonAttachmentResponse addAttachment(String lessonId, LessonAttachmentRequest request);

    void deleteAttachment(String attachmentId);
}
