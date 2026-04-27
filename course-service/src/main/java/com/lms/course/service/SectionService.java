package com.lms.course.service;

import com.lms.course.dto.request.SectionCreateRequest;
import com.lms.course.dto.response.SectionResponse;

public interface SectionService {
    SectionResponse createSection(SectionCreateRequest request);
}