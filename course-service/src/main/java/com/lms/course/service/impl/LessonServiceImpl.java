package com.lms.course.service.impl;

import com.lms.common.exception.AppException;
import com.lms.course.dto.request.LessonCreateRequest;
import com.lms.course.dto.response.LessonResponse;
import com.lms.course.entity.Lesson;
import com.lms.course.entity.Quiz;
import com.lms.course.entity.Section;
import com.lms.course.entity.VideoLesson;
import com.lms.course.repository.LessonRepository;
import com.lms.course.repository.SectionRepository;
import com.lms.course.service.LessonService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LessonServiceImpl implements LessonService {

    LessonRepository lessonRepository;
    SectionRepository sectionRepository;

    @Override
    @Transactional
    public LessonResponse createLesson(LessonCreateRequest request) {
        // 1. Kiểm tra Section có tồn tại không
        Section section = sectionRepository.findById(request.getSectionId())
                .orElseThrow(() -> new RuntimeException("Section không tồn tại!")); // Tạm dùng RuntimeException, sau này thay bằng AppException chuẩn

        Lesson lesson;

        // 2. LOGIC ĐA HÌNH: Dựa vào Type để khởi tạo đúng loại Entity
        if ("VIDEO".equalsIgnoreCase(request.getType())) {
            VideoLesson video = new VideoLesson();
            video.setVideoUrl(request.getVideoUrl());
            video.setDuration(request.getDuration() != null ? request.getDuration() : 0);
            lesson = video; // Đa hình: Ép kiểu ngược về lớp Cha
        }
        else if ("QUIZ".equalsIgnoreCase(request.getType())) {
            Quiz quiz = new Quiz();
            quiz.setPassScore(request.getPassScore() != null ? request.getPassScore() : BigDecimal.ZERO);
            lesson = quiz;
        }
        else {
            throw new RuntimeException("Loại bài học không hợp lệ. Chỉ chấp nhận VIDEO hoặc QUIZ");
        }

        // 3. Set các thông tin chung của lớp Cha (Lesson)
        lesson.setSection(section);
        lesson.setTitle(request.getTitle());
        lesson.setOrderIndex(request.getOrderIndex());
        lesson.setFreePreview(request.getFreePreview() != null ? request.getFreePreview() : false);
        // Type sẽ được tự động set nhờ hàm @PrePersist ở Entity con

        // 4. Lưu xuống DB (Hibernate sẽ tự động INSERT vào cả 2 bảng: lessons và video_lessons/quizzes)
        Lesson savedLesson = lessonRepository.save(lesson);

        // 5. Map dữ liệu ra Response
        return mapToResponse(savedLesson);
    }

    // Hàm phụ trợ dùng chung để map Entity -> DTO
    private LessonResponse mapToResponse(Lesson lesson) {
        LessonResponse response = LessonResponse.builder()
                .id(lesson.getId())
                .title(lesson.getTitle())
                .type(lesson.getType())
                .orderIndex(lesson.getOrderIndex())
                .freePreview(lesson.getFreePreview())
                .build();

        // Ép kiểu xuôi để lấy thông tin riêng biệt của từng loại
        if (lesson instanceof VideoLesson) {
            VideoLesson video = (VideoLesson) lesson;
            response.setVideoUrl(video.getVideoUrl());
            response.setDuration(video.getDuration());
        } else if (lesson instanceof Quiz) {
            Quiz quiz = (Quiz) lesson;
            response.setPassScore(quiz.getPassScore());
        }

        return response;
    }
}