package com.lms.course.controller;

import com.lms.common.dto.response.ApiResponse;
import com.lms.course.dto.request.SectionCreateRequest;
import com.lms.course.dto.response.SectionResponse;
import com.lms.course.service.SectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sections")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Section Controller", description = "Quản lý chương học (Section)")
public class SectionController {

    SectionService sectionService;

    @Operation(summary = "Tạo mới chương học", description = "Tạo một chương học mới cho một khóa học cụ thể")
    @PostMapping
    public ApiResponse<SectionResponse> createSection(@Valid @RequestBody SectionCreateRequest request) {
        SectionResponse response = sectionService.createSection(request);
        return ApiResponse.success(response);
    }
}