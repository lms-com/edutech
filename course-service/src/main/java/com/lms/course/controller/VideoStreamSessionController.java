package com.lms.course.controller;

import com.lms.common.dto.response.ApiResponse;
import com.lms.course.dto.request.VideoStreamHeartbeatRequest;
import com.lms.course.dto.response.VideoStreamHeartbeatResponse;
import com.lms.course.service.CourseAccessService;
import com.lms.course.service.VideoStreamSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/lessons")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Video Stream Session Controller", description = "Kiểm soát số lượng thiết bị phát video đồng thời")
public class VideoStreamSessionController {

    VideoStreamSessionService sessionService;
    CourseAccessService courseAccessService;

    @Operation(summary = "Gửi nhịp tim (heartbeat) khi xem video", description = "Duy trì phiên phát và kiểm tra xung đột xem đồng thời trên nhiều thiết bị")
    @PostMapping("/{lessonId}/heartbeat")
    @PreAuthorize("hasAnyAuthority('COURSE_LEARN', 'COURSE_UPDATE', 'COURSE_APPROVE', 'ADMIN')")
    public ApiResponse<VideoStreamHeartbeatResponse> heartbeat(
            @PathVariable String lessonId,
            @Valid @RequestBody VideoStreamHeartbeatRequest request,
            Authentication authentication) {
        courseAccessService.requireLessonAccess(lessonId, authentication);
        String userId = authentication.getName();
        VideoStreamHeartbeatResponse response = sessionService.processHeartbeat(lessonId, userId, request);
        return ApiResponse.success(response);
    }

    @Operation(summary = "Chiếm quyền phát video trên thiết bị này", description = "Khi bị cảnh báo thiết bị khác đang xem, cho phép học viên xác nhận tiếp tục phát trên thiết bị hiện tại")
    @PostMapping("/{lessonId}/claim-session")
    @PreAuthorize("hasAnyAuthority('COURSE_LEARN', 'COURSE_UPDATE', 'COURSE_APPROVE', 'ADMIN')")
    public ApiResponse<VideoStreamHeartbeatResponse> claimSession(
            @PathVariable String lessonId,
            @Valid @RequestBody VideoStreamHeartbeatRequest request,
            Authentication authentication) {
        courseAccessService.requireLessonAccess(lessonId, authentication);
        String userId = authentication.getName();
        VideoStreamHeartbeatResponse response = sessionService.claimSession(lessonId, userId, request);
        return ApiResponse.success(response);
    }

    @Operation(summary = "Giải phóng phiên xem video khi dừng học", description = "Xóa khóa phiên xem trong Redis khi đóng bài học hoặc tắt player")
    @PostMapping("/{lessonId}/release-session")
    @PreAuthorize("hasAnyAuthority('COURSE_LEARN', 'COURSE_UPDATE', 'COURSE_APPROVE', 'ADMIN')")
    public ApiResponse<Void> releaseSession(
            @PathVariable String lessonId,
            @RequestParam(required = false) String sessionId,
            Authentication authentication) {
        String userId = authentication.getName();
        sessionService.releaseSession(userId, sessionId);
        return ApiResponse.success(null);
    }
}
