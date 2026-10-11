package com.lms.finance.controller.internal;

import com.lms.finance.entity.InstructorBalance;
import com.lms.finance.repository.InstructorBalanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/internal/finance")
@RequiredArgsConstructor
@Slf4j
public class FinanceBackfillController {

    private final InstructorBalanceRepository balanceRepository;
    private final RabbitTemplate rabbitTemplate;

    @PostMapping("/sync-redis")
    public String syncBalancesToAnalytics() {
        int page = 0;
        int size = 500; // Batch size lý tưởng
        int totalProcessed = 0;
        Page<InstructorBalance> balancePage;

        do {
            balancePage = balanceRepository.findAll(PageRequest.of(page, size));

            for (InstructorBalance balance : balancePage.getContent()) {
                Map<String, Object> message = Map.of(
                        "instructorId", balance.getInstructorId(),
                        "availableBalance", balance.getAvailableBalance()
                );
                rabbitTemplate.convertAndSend("finance.exchange", "finance.balance.updated", message);
                totalProcessed++;
            }
            log.info("Đã đồng bộ batch {} của Finance. Tổng: {}", page, totalProcessed);
            page++;

        } while (balancePage.hasNext());

        return "✅ Hoàn tất đồng bộ " + totalProcessed + " số dư giảng viên an toàn.";
    }
}
