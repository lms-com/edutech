package com.lms.course.service;

import com.lms.course.dto.request.VideoStreamHeartbeatRequest;
import com.lms.course.dto.response.VideoStreamHeartbeatResponse;

public interface VideoStreamSessionService {

    VideoStreamHeartbeatResponse processHeartbeat(String lessonId, String userId, VideoStreamHeartbeatRequest request);

    VideoStreamHeartbeatResponse claimSession(String lessonId, String userId, VideoStreamHeartbeatRequest request);

    void releaseSession(String userId, String sessionId);
}
