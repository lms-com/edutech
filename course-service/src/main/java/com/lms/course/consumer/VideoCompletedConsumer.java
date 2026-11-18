package com.lms.course.consumer;

import com.lms.course.config.CacheConfig;
import com.lms.course.config.RabbitMQConfig;
import com.lms.course.dto.message.VideoCompletedMessage;
import com.lms.course.entity.VideoLesson;
import com.lms.course.repository.LessonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Lắng nghe sự kiện video đã hoàn tất chuyển mã HLS từ video-worker-service.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VideoCompletedConsumer {

    private final LessonRepository lessonRepository;
    private final CacheManager cacheManager;

    @RabbitListener(queues = RabbitMQConfig.VIDEO_COMPLETED_QUEUE)
    @Transactional(readOnly = true)
    public void onVideoCompleted(VideoCompletedMessage message) {
        if (message == null || message.getMediaId() == null || message.getMediaId().isBlank()) {
            log.warn("⚠️ Nhận được VideoCompletedMessage rỗng hoặc thiếu mediaId.");
            return;
        }
        String mediaId = message.getMediaId().trim();
        log.info("🎬 [COURSE] Nhận sự kiện video hoàn tất xử lý HLS: mediaId={}", mediaId);

        List<VideoLesson> lessons = lessonRepository.findVideoLessonsByMediaId(mediaId);
        Cache courseDetailCache = cacheManager.getCache(CacheConfig.CACHE_COURSE_DETAIL);
        for (VideoLesson lesson : lessons) {
            if (lesson.getSection() != null && lesson.getSection().getCourse() != null) {
                String courseId = lesson.getSection().getCourse().getId();
                if (courseDetailCache != null) {
                    courseDetailCache.evict(courseId);
                }
                log.info("✅ [COURSE] Đã làm mới cache khóa học [{}] cho bài học [{}] (mediaId={})",
                        courseId, lesson.getId(), mediaId);
            }
        }
    }
}
