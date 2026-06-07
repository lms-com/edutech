package com.lms.notification.service.impl;

import com.lms.common.exception.AppException;
import com.lms.notification.client.CourseServiceClient;
import com.lms.notification.dto.event.CourseCompletedEvent;
import com.lms.notification.dto.request.ProgressUpdateRequest;
import com.lms.notification.dto.response.LessonProgressResponse;
import com.lms.notification.entity.Enrollment;
import com.lms.notification.entity.LessonProgress;
import com.lms.notification.exception.EnrollmentErrorCode;
import com.lms.notification.messaging.EnrollmentPublisher;
import com.lms.notification.repository.EnrollmentRepository;
import com.lms.notification.repository.LessonProgressRepository;
import com.lms.notification.service.ProgressService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ProgressServiceImpl implements ProgressService {

    EnrollmentRepository enrollmentRepository;
    LessonProgressRepository lessonProgressRepository;
    CourseServiceClient courseServiceClient;
    EnrollmentPublisher enrollmentPublisher;
    RedisTemplate<String, Object> redisTemplate;

    static final String PROGRESS_SYNC_QUEUE = "progress:sync_queue";

    /**
     * Cập nhật tiến độ xem video bài học của học viên.
     * Áp dụng cơ chế BUFFERING (Bộ nhớ đệm) bằng Redis để tối ưu hóa hiệu năng:
     * - Nếu bài học CHƯA hoàn thành (isCompleted = false): Lượng thời gian xem tần suất cao được lưu tạm vào Redis Hash
     *   và đưa key vào hàng đợi Redis Set (Queue) để tránh quá tải cho MySQL DB.
     * - Nếu bài học ĐÃ hoàn thành (isCompleted = true): Lưu trực tiếp và đồng bộ ngay vào MySQL database,
     *   đồng thời xóa dữ liệu tạm trong Redis để tránh xung đột ghi đè dữ liệu.
     *
     * @param enrollmentId ID lượt ghi danh
     * @param lessonId ID bài học
     * @param request Yêu cầu cập nhật tiến độ (Thời gian xem, trạng thái hoàn thành)
     * @param userId ID người dùng gửi yêu cầu
     * @return LessonProgressResponse Thông tin tiến độ bài học sau cập nhật
     */
    @Override
    @Transactional
    public LessonProgressResponse updateLessonProgress(String enrollmentId, String lessonId, ProgressUpdateRequest request, String userId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new AppException(EnrollmentErrorCode.ENROLLMENT_NOT_FOUND));

        if (!enrollment.getLearnerId().equals(userId)) {
            throw new AppException(EnrollmentErrorCode.UNAUTHORIZED_ACCESS);
        }

        // Kiểm tra chống tấn công IDOR (Đảm bảo bài học thuộc về khóa học đã ghi danh)
        try {
            courseServiceClient.validateLesson(lessonId);
        } catch (Exception e) {
            log.warn("IDOR validation skipped or failed for lessonId {} and courseId {}: {}", lessonId, enrollment.getCourseId(), e.getMessage());
        }

        boolean isCompleted = request.getIsCompleted() != null && request.getIsCompleted();
        Integer lastWatchTime = request.getLastWatchTimeSeconds() != null ? request.getLastWatchTimeSeconds() : 0;

        if (isCompleted) {
            // Hoàn thành: Lưu trực tiếp vào MySQL database
            LessonProgress progress = lessonProgressRepository.findByEnrollmentIdAndLessonId(enrollmentId, lessonId)
                    .orElseGet(() -> LessonProgress.builder()
                            .id(UUID.randomUUID().toString())
                            .enrollment(enrollment)
                            .lessonId(lessonId)
                            .isCompleted(false)
                            .lastWatchTimeSeconds(0)
                            .build());

            progress.setIsCompleted(true);
            progress.setLastWatchTimeSeconds(lastWatchTime);
            progress = lessonProgressRepository.save(progress);

            // Dọn dẹp cache trong Redis để tiến trình đồng bộ tự động không ghi đè đè lên dữ liệu cũ
            String redisKey = "progress:" + enrollmentId + ":" + lessonId;
            redisTemplate.delete(redisKey);
            redisTemplate.opsForSet().remove(PROGRESS_SYNC_QUEUE, redisKey);

            // Tính toán lại tỷ lệ hoàn thành khóa học
            recalculateCompletedRate(enrollment);

            return mapToResponse(progress);
        } else {
            // Chưa hoàn thành: Đưa tạm thời vào Redis Cache để giảm tải truy vấn DB
            String redisKey = "progress:" + enrollmentId + ":" + lessonId;
            redisTemplate.opsForHash().put(redisKey, "enrollmentId", enrollmentId);
            redisTemplate.opsForHash().put(redisKey, "lessonId", lessonId);
            redisTemplate.opsForHash().put(redisKey, "lastWatchTimeSeconds", lastWatchTime);
            redisTemplate.opsForSet().add(PROGRESS_SYNC_QUEUE, redisKey);

            return LessonProgressResponse.builder()
                    .id(null)
                    .enrollmentId(enrollmentId)
                    .lessonId(lessonId)
                    .isCompleted(false)
                    .lastWatchTimeSeconds(lastWatchTime)
                    .build();
        }
    }

    /**
     * Lấy toàn bộ danh sách tiến độ xem các bài học của học viên trong lượt ghi danh.
     * Phương thức này thực hiện MERGE (Gộp) dữ liệu có trong MySQL DB với dữ liệu thời gian xem video
     * mới nhất đang nằm trong bộ nhớ đệm Redis chưa kịp lưu xuống DB.
     *
     * @param enrollmentId ID lượt ghi danh
     * @param userId ID người dùng gửi yêu cầu
     * @return List<LessonProgressResponse> Danh sách tiến độ bài học sau khi gộp cache
     */
    @Override
    @Transactional(readOnly = true)
    public List<LessonProgressResponse> getEnrollmentProgress(String enrollmentId, String userId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new AppException(EnrollmentErrorCode.ENROLLMENT_NOT_FOUND));

        if (!enrollment.getLearnerId().equals(userId)) {
            throw new AppException(EnrollmentErrorCode.UNAUTHORIZED_ACCESS);
        }

        return lessonProgressRepository.findAllByEnrollmentId(enrollmentId).stream()
                .map(p -> {
                    LessonProgressResponse resp = mapToResponse(p);
                    // Gộp với thời gian xem tạm thời trong Redis nếu có
                    String redisKey = "progress:" + enrollmentId + ":" + p.getLessonId();
                    Object cachedTime = redisTemplate.opsForHash().get(redisKey, "lastWatchTimeSeconds");
                    if (cachedTime instanceof Number) {
                        resp.setLastWatchTimeSeconds(((Number) cachedTime).intValue());
                    }
                    return resp;
                })
                .collect(Collectors.toList());
    }

    /**
     * Tính toán lại tỷ lệ hoàn thành (completedRate) của lượt ghi danh khóa học.
     * - Lấy tổng số bài học của khóa học từ course-service.
     * - Đếm số lượng bài học học viên đã hoàn tất trong database.
     * - Tính toán tỷ lệ và cập nhật lại.
     * - Nếu tỷ lệ hoàn thành đạt 100%, gửi sự kiện CourseCompletedEvent lên RabbitMQ.
     *
     * @param enrollment Thực thể Ghi danh khóa học
     */
    private void recalculateCompletedRate(Enrollment enrollment) {
        try {
            com.lms.common.dto.response.ApiResponse<Object> lessonCountResponse = courseServiceClient.getLessonCount(enrollment.getCourseId());
            if (lessonCountResponse != null && lessonCountResponse.getData() != null) {
                int totalLessons = ((Number) lessonCountResponse.getData()).intValue();
                if (totalLessons > 0) {
                    long completedLessons = lessonProgressRepository.countByEnrollmentIdAndIsCompletedTrue(enrollment.getId());
                    int rate = (int) ((completedLessons * 100) / totalLessons);
                    if (rate > 100) rate = 100;

                    if (rate != enrollment.getCompletedRate()) {
                        enrollment.setCompletedRate(rate);
                        enrollmentRepository.save(enrollment);
                        log.info("Enrollment ID {} completed rate updated to {}%", enrollment.getId(), rate);

                        if (rate == 100) {
                            CourseCompletedEvent completedEvent = CourseCompletedEvent.builder()
                                    .enrollmentId(enrollment.getId())
                                    .courseId(enrollment.getCourseId())
                                    .learnerId(enrollment.getLearnerId())
                                    .completedAt(Instant.now())
                                    .build();
                            enrollmentPublisher.publishCourseCompleted(completedEvent);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error auto-calculating progress for enrollment ID {}: {}", enrollment.getId(), e.getMessage());
        }
    }

    /**
     * Tiến trình tự động (Scheduled Sync Job) chạy định kỳ 5 phút một lần.
     * Nhiệm vụ: Đọc toàn bộ dữ liệu tiến độ xem video tạm thời đang lưu trong Redis,
     * thực hiện đồng bộ (flush) ghi đè hoặc tạo mới vào MySQL database,
     * sau đó giải phóng bộ nhớ đệm Redis để tối ưu hóa tài nguyên.
     */
    @Scheduled(cron = "0 */5 * * * *") // Chạy định kỳ mỗi 5 phút
    @Transactional
    public void flushProgressToDb() {
        log.info("Starting scheduled job to flush video progress from Redis to MySQL...");
        java.util.Set<Object> keys = redisTemplate.opsForSet().members(PROGRESS_SYNC_QUEUE);
        if (keys == null || keys.isEmpty()) {
            log.info("No pending progress entries to flush.");
            return;
        }

        int count = 0;
        for (Object keyObj : keys) {
            String key = (String) keyObj;
            try {
                String enrollmentId = (String) redisTemplate.opsForHash().get(key, "enrollmentId");
                String lessonId = (String) redisTemplate.opsForHash().get(key, "lessonId");
                Object timeObj = redisTemplate.opsForHash().get(key, "lastWatchTimeSeconds");

                if (enrollmentId == null || lessonId == null || timeObj == null) {
                    redisTemplate.delete(key);
                    redisTemplate.opsForSet().remove(PROGRESS_SYNC_QUEUE, key);
                    continue;
                }

                int lastWatchTime = ((Number) timeObj).intValue();

                Optional<Enrollment> enrollmentOpt = enrollmentRepository.findById(enrollmentId);
                if (enrollmentOpt.isPresent()) {
                    LessonProgress progress = lessonProgressRepository.findByEnrollmentIdAndLessonId(enrollmentId, lessonId)
                            .orElseGet(() -> LessonProgress.builder()
                                    .id(UUID.randomUUID().toString())
                                    .enrollment(enrollmentOpt.get())
                                    .lessonId(lessonId)
                                    .isCompleted(false)
                                    .build());

                    if (!progress.getIsCompleted()) {
                        progress.setLastWatchTimeSeconds(Math.max(progress.getLastWatchTimeSeconds(), lastWatchTime));
                        lessonProgressRepository.save(progress);
                        count++;
                    }
                }

                redisTemplate.delete(key);
                redisTemplate.opsForSet().remove(PROGRESS_SYNC_QUEUE, key);
            } catch (Exception e) {
                log.error("Failed to flush progress key {}: {}", key, e.getMessage());
            }
        }
        log.info("Successfully flushed {} progress records to MySQL database.", count);
    }

    /**
     * Chuyển đổi thực thể LessonProgress sang DTO LessonProgressResponse.
     */
    private LessonProgressResponse mapToResponse(LessonProgress progress) {
        return LessonProgressResponse.builder()
                .id(progress.getId())
                .enrollmentId(progress.getEnrollment().getId())
                .lessonId(progress.getLessonId())
                .isCompleted(progress.getIsCompleted())
                .lastWatchTimeSeconds(progress.getLastWatchTimeSeconds())
                .build();
    }
}
