package com.lms.course.service;

import com.lms.course.dto.request.SectionCreateRequest;
import com.lms.course.dto.request.SectionUpdateRequest;
import com.lms.course.dto.response.SectionResponse;

import java.util.List;

public interface SectionService {
    SectionResponse createSection(SectionCreateRequest request);

    void updateSection(String sectionId, SectionUpdateRequest request);

    void deleteSection(String sectionId);

    void reorderSections(String courseId, List<String> orderedIds);
}