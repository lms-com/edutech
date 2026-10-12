package com.lms.iam.controller.internal;

import com.lms.iam.model.User;
import com.lms.iam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static com.lms.iam.config.RabbitMQConfig.IAM_EXCHANGE;
import static com.lms.iam.config.RabbitMQConfig.IAM_USER_UPDATED_ROUTING_KEY;

@RestController
@RequestMapping("/api/internal/v1/iam")
@RequiredArgsConstructor
@Slf4j
public class IamBackfillController {

    private final UserRepository userRepository;
    private final RabbitTemplate rabbitTemplate;

    @PostMapping("/sync-redis")
    public String syncUsersToAnalytics() {
        int page = 0;
        int size = 500;
        int totalProcessed = 0;
        Page<User> userPage;

        do {
            userPage = userRepository.findAll(PageRequest.of(page, size));

            for (User user : userPage.getContent()) {
                Map<String, Object> message = Map.of(
                        "userId", user.getId(),
                        "fullName", user.getFullName() != null ? user.getFullName() : "Ẩn danh",
                        // ĐÃ CẬP NHẬT: Lấy avatarUrl từ User
                        "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "",
                        "status", user.getStatus() != null ? user.getStatus().name() : "UNKNOWN"
                );

                rabbitTemplate.convertAndSend(IAM_EXCHANGE, IAM_USER_UPDATED_ROUTING_KEY, message);
                totalProcessed++;
            }
            log.info("Đã đồng bộ batch {} của IAM. Tổng: {}", page, totalProcessed);
            page++;

        } while (userPage.hasNext());

        return "✅ Hoàn tất đồng bộ " + totalProcessed + " users an toàn.";
    }
}
