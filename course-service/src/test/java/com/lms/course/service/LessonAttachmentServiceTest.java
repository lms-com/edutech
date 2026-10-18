package com.lms.course.service;

import com.lms.common.exception.AppException;
import com.lms.course.dto.request.LessonAttachmentRequest;
import com.lms.course.dto.response.LessonAttachmentResponse;
import com.lms.course.entity.Lesson;
import com.lms.course.entity.LessonAttachment;
import com.lms.course.entity.VideoLesson;
import com.lms.course.repository.LessonAttachmentRepository;
import com.lms.course.repository.LessonRepository;
import com.lms.course.service.impl.LessonAttachmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LessonAttachmentServiceTest {

    @Mock
    private LessonAttachmentRepository attachmentRepository;

    @Mock
    private LessonRepository lessonRepository;

    private LessonAttachmentServiceImpl attachmentService;

    @BeforeEach
    void setUp() {
        attachmentService = new LessonAttachmentServiceImpl(attachmentRepository, lessonRepository);
    }

    @Test
    @DisplayName("Lấy danh sách tài liệu bài học thành công")
    void testGetAttachments_Success() {
        when(lessonRepository.existsById("lesson-1")).thenReturn(true);

        Lesson lesson = new VideoLesson();
        lesson.setId("lesson-1");

        LessonAttachment a1 = LessonAttachment.builder()
                .id("att-1")
                .lesson(lesson)
                .name("Slide 1")
                .fileUrl("https://example.com/slide1.pdf")
                .fileSize("2 MB")
                .fileType("PDF")
                .build();

        when(attachmentRepository.findByLessonIdOrderByCreatedAtAsc("lesson-1"))
                .thenReturn(List.of(a1));

        List<LessonAttachmentResponse> result = attachmentService.getAttachments("lesson-1");

        assertEquals(1, result.size());
        assertEquals("att-1", result.get(0).getId());
        assertEquals("Slide 1", result.get(0).getName());
        assertEquals("PDF", result.get(0).getFileType());
    }

    @Test
    @DisplayName("Thêm tài liệu mới cho bài học thành công")
    void testAddAttachment_Success() {
        Lesson lesson = new VideoLesson();
        lesson.setId("lesson-1");
        when(lessonRepository.findById("lesson-1")).thenReturn(Optional.of(lesson));

        LessonAttachment saved = LessonAttachment.builder()
                .id("att-new")
                .lesson(lesson)
                .name("Code demo")
                .fileUrl("https://example.com/code.zip")
                .fileSize("15 MB")
                .fileType("ZIP")
                .build();

        when(attachmentRepository.save(any(LessonAttachment.class))).thenReturn(saved);

        LessonAttachmentRequest req = LessonAttachmentRequest.builder()
                .name("Code demo")
                .fileUrl("https://example.com/code.zip")
                .fileSize("15 MB")
                .fileType("ZIP")
                .build();

        LessonAttachmentResponse result = attachmentService.addAttachment("lesson-1", req);

        assertNotNull(result);
        assertEquals("att-new", result.getId());
        assertEquals("Code demo", result.getName());
        assertEquals("ZIP", result.getFileType());
    }

    @Test
    @DisplayName("Xóa tài liệu đính kèm thành công")
    void testDeleteAttachment_Success() {
        LessonAttachment a = LessonAttachment.builder().id("att-1").build();
        when(attachmentRepository.findById("att-1")).thenReturn(Optional.of(a));

        assertDoesNotThrow(() -> attachmentService.deleteAttachment("att-1"));
        verify(attachmentRepository).delete(a);
    }
}
