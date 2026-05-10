package com.lms.course.service.impl;

import com.lms.common.dto.response.ApiResponse;
import com.lms.common.exception.AppException;
import com.lms.course.client.MediaServiceClient;
import com.lms.course.dto.request.LessonCreateRequest;
import com.lms.course.dto.request.LessonUpdateContentRequest;
import com.lms.course.dto.response.LessonResponse;
import com.lms.course.entity.Lesson;
import com.lms.course.entity.Quiz;
import com.lms.course.entity.Section;
import com.lms.course.entity.VideoLesson;
import com.lms.course.exception.CourseErrorCode;
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
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LessonServiceImpl implements LessonService {

    LessonRepository lessonRepository;
    SectionRepository sectionRepository;
    MediaServiceClient mediaServiceClient;

    @Override
    @Transactional
    public LessonResponse createLesson(LessonCreateRequest request) {
        // 1. Kiểm tra Section có tồn tại không
        Section section = sectionRepository.findById(request.getSectionId())
                .orElseThrow(() -> new AppException(CourseErrorCode.SECTION_NOT_FOUND));

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
            throw new AppException(CourseErrorCode.LESSON_INVALID_TYPE);
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

    @Override
    @Transactional
    public void updateLessonContent(String lessonId, LessonUpdateContentRequest request) {
        // 1. Kiểm tra Lesson có tồn tại không
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new AppException(CourseErrorCode.LESSON_NOT_FOUND));
                
        if (request.getTitle() != null) {
            lesson.setTitle(request.getTitle());
        }
        if (request.getFreePreview() != null) {
            lesson.setFreePreview(request.getFreePreview());
        }

        if (lesson instanceof VideoLesson) {
            VideoLesson videoLesson = (VideoLesson) lesson;
            if (request.getVideoUrl() != null) {
                videoLesson.setVideoUrl(request.getVideoUrl());
            }
            if (request.getDuration() != null) {
                videoLesson.setDuration(request.getDuration());
            }
        } else if (lesson instanceof Quiz) {
            Quiz quiz = (Quiz) lesson;
            if (request.getPassScore() != null) {
                quiz.setPassScore(request.getPassScore());
            }
        }

        lessonRepository.save(lesson);
    }

    @Override
    @Transactional
    public void deleteLesson(String lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new AppException(CourseErrorCode.LESSON_NOT_FOUND));
        
        lesson.setDeleted(true);
        lessonRepository.save(lesson);
    }

    @Override
    @Transactional
    public void reorderLessons(String sectionId, List<String> orderedIds) {
        // 1. Check Section exists
        sectionRepository.findById(sectionId)
                .orElseThrow(() -> new AppException(CourseErrorCode.SECTION_NOT_FOUND));

        if (orderedIds == null || orderedIds.isEmpty()) return;

        // 2. Fetch all lessons by IDs to ensure they exist and belong to the section (optional but good practice)
        List<Lesson> lessons = lessonRepository.findAllById(orderedIds);
        
        // Cập nhật lại orderIndex dựa theo thứ tự list gửi lên
        for (int i = 0; i < orderedIds.size(); i++) {
            String id = orderedIds.get(i);
            Lesson lesson = lessons.stream()
                .filter(l -> l.getId().equals(id))
                .findFirst()
                .orElse(null);
                
            if (lesson != null && lesson.getSection().getId().equals(sectionId)) {
                lesson.setOrderIndex(i);
            }
        }
        
        lessonRepository.saveAll(lessons);
    }

    @Override
    public String getPlayUrl(String lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new AppException(CourseErrorCode.LESSON_NOT_FOUND));

        if (!(lesson instanceof VideoLesson)) {
            throw new AppException(CourseErrorCode.LESSON_INVALID_TYPE);
        }

        VideoLesson videoLesson = (VideoLesson) lesson;
        String videoPath = videoLesson.getVideoUrl();

        if (videoPath == null || videoPath.isEmpty()) {
             throw new AppException(CourseErrorCode.LESSON_CONTENT_MISSING);
        }

        // Gọi sang Media Service thông qua FeignClient
        ApiResponse<String> response = mediaServiceClient.getViewUrl(videoPath);
        
        if (response != null && response.getCode() == 200) {
            return response.getData();
        } else {
             // Có thể ném lỗi từ media service nếu lấy URL thất bại
             throw new AppException(CourseErrorCode.INTERNAL_SERVER_ERROR);
        }
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
