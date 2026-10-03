package com.lms.notification.service.impl;

import com.lms.notification.service.SseEmitterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class SseEmitterServiceImpl implements SseEmitterService {

    // ConcurrentHashMap là bắt buộc để quản lý bộ nhớ Thread-safe trong môi trường xử lý đồng thời (Concurrency)
    // Key: userId, Value: Kết nối SseEmitter tương ứng của user đó
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    // Cấu hình thời gian sống của kết nối là 15 phút (900,000 mili-giây)
    private static final Long TIMEOUT = 900000L;

    @Override
    public SseEmitter subscribe(String userId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT);

        // Lưu kết nối của học viên này vào bộ nhớ RAM tạm thời của Server
        this.emitters.put(userId, emitter);
        log.info("Học viên [{}] đã thiết lập kết nối quả chuông Real-time thành công. Hiện tại có {} kết nối active.", userId, emitters.size());

        // Các chốt chặn dọn dẹp bộ nhớ khi kết nối kết thúc, lỗi hoặc quá hạn (Timeout)
        emitter.onCompletion(() -> {
            this.emitters.remove(userId);
            log.info("Kết nối của học viên [{}] kết thúc (Completion). Dọn dẹp RAM.", userId);
        });

        emitter.onTimeout(() -> {
            this.emitters.remove(userId);
            log.warn("Kết nối của học viên [{}] bị quá hạn (Timeout). Tự động ngắt để bảo vệ tài nguyên.", userId);
        });

        emitter.onError((ex) -> {
            this.emitters.remove(userId);
            log.error("Kết nối của học viên [{}] gặp lỗi hệ thống. Đã gỡ bỏ connection khỏi map.", userId);
        });

        // MẸO FINTECH/SYSTEM: Gửi ngay 1 event mồi "INIT" xuống Client.
        // Nếu không có tin nhắn đầu tiên này, một số Proxy (như Nginx) hoặc API Gateway sẽ tự động ngắt kết nối sau 30s vì nghĩ kết nối bị treo (504 Gateway Timeout).
        try {
            emitter.send(SseEmitter.event()
                    .name("INIT")
                    .data("Đã thiết lập đường ống nhận thông báo thành công!"));
        } catch (IOException e) {
            this.emitters.remove(userId);
        }

        return emitter;
    }

    @Override
    public void sendNotification(String userId, Object payload) {
        SseEmitter emitter = this.emitters.get(userId);
        if (emitter != null) {
            try {
                // Đẩy dữ liệu real-time xuống qua event tên là "NEW_NOTIFICATION"
                emitter.send(SseEmitter.event()
                        .name("NEW_NOTIFICATION")
                        .data(payload));
                log.info("Đã đẩy thông báo real-time thành công tới màn hình của User: [{}]", userId);
            } catch (IOException e) {
                // Nếu đẩy lỗi (ví dụ user đã tắt tab trình duyệt đột ngột), dọn dẹp connection ngay
                this.emitters.remove(userId);
                log.warn("Không thể gửi real-time cho User [{}], có thể họ đã tắt trình duyệt. Đã xóa kết nối lỗi.", userId);
            }
        } else {
            log.info("User [{}] hiện không online (không mở tab học). Thông báo sẽ chỉ lưu DB chứ không push real-time.", userId);
        }
    }
}