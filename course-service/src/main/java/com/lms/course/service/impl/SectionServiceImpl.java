package com.lms.course.service.impl;

import com.lms.course.dto.request.SectionCreateRequest;
import com.lms.course.dto.response.SectionResponse;
import com.lms.course.entity.Course;
import com.lms.course.entity.Section;
import com.lms.course.repository.CourseRepository;
import com.lms.course.repository.SectionRepository;
import com.lms.course.service.SectionService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SectionServiceImpl implements SectionService {

    SectionRepository sectionRepository;
    CourseRepository courseRepository;

    @Override
    @Transactional
    public SectionResponse createSection(SectionCreateRequest request) {
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new RuntimeException("Course không tồn tại!"));

        Section section = Section.builder()
                .course(course)
                .title(request.getTitle())
                .orderIndex(request.getOrderIndex())
                .build();

        Section savedSection = sectionRepository.save(section);

        return SectionResponse.builder()
                .id(savedSection.getId())
                .courseId(savedSection.getCourse().getId())
                .title(savedSection.getTitle())
                .orderIndex(savedSection.getOrderIndex())
                .build();
    }
}