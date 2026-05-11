package com.lms.course.service.impl;

import com.lms.common.exception.AppException;
import com.lms.course.dto.request.SectionCreateRequest;
import com.lms.course.dto.request.SectionUpdateRequest;
import com.lms.course.dto.response.SectionResponse;
import com.lms.course.entity.Course;
import com.lms.course.entity.Lesson;
import com.lms.course.entity.Section;
import com.lms.course.exception.CourseErrorCode;
import com.lms.course.repository.CourseRepository;
import com.lms.course.repository.LessonRepository;
import com.lms.course.repository.SectionRepository;
import com.lms.course.service.SectionService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SectionServiceImpl implements SectionService {

    SectionRepository sectionRepository;
    CourseRepository courseRepository;
    LessonRepository lessonRepository;

    /**
     * Tạo một chương học mới cho một khóa học.
     * @param request DTO chứa thông tin chương học cần tạo (courseId, title, orderIndex).
     * @return DTO chứa thông tin chi tiết của chương học vừa được tạo.
     */
    @Override
    @Transactional
    public SectionResponse createSection(SectionCreateRequest request) {
        // Tìm kiếm khóa học theo ID, nếu không thấy thì báo lỗi
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new AppException(CourseErrorCode.COURSE_NOT_FOUND));

        // Xây dựng đối tượng Section từ request
        Section section = Section.builder()
                .course(course)
                .title(request.getTitle())
                .orderIndex(request.getOrderIndex())
                .build();

        // Lưu section mới vào DB
        Section savedSection = sectionRepository.save(section);

        // Map entity đã lưu sang DTO để trả về cho client
        return SectionResponse.builder()
                .id(savedSection.getId())
                .courseId(savedSection.getCourse().getId())
                .title(savedSection.getTitle())
                .orderIndex(savedSection.getOrderIndex())
                .build();
    }

    /**
     * Cập nhật thông tin (chủ yếu là tiêu đề) của một chương học.
     * @param sectionId ID của chương học cần cập nhật.
     * @param request DTO chứa thông tin mới cần cập nhật.
     */
    @Override
    @Transactional
    public void updateSection(String sectionId, SectionUpdateRequest request){
        // Tìm chương học theo ID, nếu không thấy thì báo lỗi
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new AppException(CourseErrorCode.SECTION_NOT_FOUND));

        // Cập nhật tiêu đề mới cho chương học
        section.setTitle(request.getTitle());

        // Lưu lại thay đổi
        sectionRepository.save(section);
    }

    /**
     * Xóa mềm một chương học và tất cả các bài học (Lesson) bên trong nó.
     * @param sectionId ID của chương học cần xóa.
     */
    @Override
    @Transactional
    public void deleteSection(String sectionId){
        // Tìm chương học theo ID, nếu không thấy thì báo lỗi
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new AppException(CourseErrorCode.SECTION_NOT_FOUND));

        // 1. Xóa mềm chính Chương học đó
        section.setDeleted(true);
        sectionRepository.save(section);

        // 2. Xóa mềm toàn bộ Bài học (Lesson) thuộc Chương này
        List<Lesson> lessons = lessonRepository.findBySectionIdAndDeletedFalse(sectionId);
        lessons.forEach(lesson -> lesson.setDeleted(true));
        lessonRepository.saveAll(lessons); // Save 1 cục cho tối ưu
    }

    /**
     * Sắp xếp lại thứ tự của các chương học trong một khóa học.
     * @param courseId ID của khóa học chứa các chương cần sắp xếp.
     * @param orderedIds Danh sách các ID của chương học theo thứ tự mới.
     */
    @Override
    @Transactional
    public void reorderSections(String courseId, List<String> orderedIds){
        // Duyệt qua danh sách các ID đã được sắp xếp từ client
        for (int i = 0; i < orderedIds.size(); i++) {
            String id = orderedIds.get(i);
            int newOrderIndex = i; // Tạo biến copy để tránh lỗi "effectively final" trong lambda
            // Dùng findById là đủ vì trong payload chỉ gửi ID của các section hợp lệ
            sectionRepository.findById(id).ifPresent(section -> {
                // Cập nhật lại orderIndex theo vị trí mới trong mảng
                section.setOrderIndex(newOrderIndex);
                sectionRepository.save(section);
            });
        }
    }
}
