package com.lms.finance.service.cron;

import com.lms.finance.entity.RevenueShare;
import com.lms.finance.enums.RevenueShareStatus;
import com.lms.finance.repository.RevenueShareRepository;
import com.lms.finance.service.InstructorBalanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class RevenueReleaseCronJob {
    private final InstructorBalanceService balanceService;
    private final RevenueShareRepository revenueShareRepository;

    @Scheduled(cron = "${application.cron.balance-settlement}")
    public void runReleaseRevenueTask () {
        log.info("🚀 [CRONJOB] Start Release-revenueShare Processing...");
        try {
            // Lay thoi gian hien tai:
            Instant now = Instant.now();
            // Lay danh sach revenueShare du dieu kien de release:
            List<RevenueShare> releasableRevenues = revenueShareRepository.findByStatusAndReleaseAtLessThanEqual(
                    RevenueShareStatus.HOLDING,
                    now
            );

            if (releasableRevenues.isEmpty()) {
                log.info("↩️ [CRONJOB] There is no releasable revenueShare with status [\033[1;36mHOLDING\033[0m] today.");
                return;
            }

            // Tien hanh release:
            balanceService.releaseRevenue(releasableRevenues);
            log.info("✅ [CRONJOB] Release revenueShare processing complete.");
        } catch (Exception e) {
            log.error("❌ [CRONJOB] a serious Error occurred during the process.", e);
        }
    }
}
