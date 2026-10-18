package com.lms.finance.client.listener;

import com.lms.finance.dto.event.InstructorApprovedEvent;
import com.lms.finance.entity.InstructorBalance;
import com.lms.finance.repository.InstructorBalanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import static com.lms.finance.config.RabbitMQConfig.INSTRUCTOR_APPROVED_FINANCE_QUEUE;

@Component
@Lazy(false)
@Slf4j
@RequiredArgsConstructor
public class InstructorApprovedListener {

    private final InstructorBalanceRepository balanceRepository;

    @RabbitListener(queues = INSTRUCTOR_APPROVED_FINANCE_QUEUE)
    public void onInstructorApproved(InstructorApprovedEvent event) {
        log.info("📩 Nhận sự kiện InstructorApprovedEvent cho giảng viên: {}", event);
        if (event == null || event.getUserId() == null || event.getUserId().isBlank()) {
            log.warn("⚠️ Bỏ qua InstructorApprovedEvent không hợp lệ (userId null)");
            return;
        }

        String instructorId = event.getUserId();
        try {
            boolean exists = balanceRepository.findByInstructorId(instructorId).isPresent();
            if (exists) {
                log.info("ℹ️ Giảng viên {} đã có ví trong hệ thống finance, bỏ qua khởi tạo.", instructorId);
                return;
            }

            InstructorBalance newWallet = InstructorBalance.createNewWallet(instructorId);
            balanceRepository.save(newWallet);
            log.info("✅ Khởi tạo thành công ví InstructorBalance (0đ) cho giảng viên mới [userId: {}]", instructorId);
        } catch (Exception e) {
            log.error("❌ Lỗi khi khởi tạo ví cho giảng viên {}: {}", instructorId, e.getMessage(), e);
            throw e;
        }
    }
}
