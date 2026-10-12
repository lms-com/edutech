package com.lms.finance.controller.admin;

import com.lms.finance.entity.FailedSyncEvent;
import com.lms.finance.repository.FailedSyncEventRepository;
import com.lms.common.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/finance/admin/dlq")
@RequiredArgsConstructor
public class FailedSyncEventController {

    private final FailedSyncEventRepository repository;
    private final RabbitTemplate rabbitTemplate;

    @GetMapping
    public ApiResponse<List<FailedSyncEvent>> getFailedEvents() {
        // Lấy danh sách các lỗi chưa xử lý
        return ApiResponse.success(repository.findAll());
    }

    @PostMapping("/{id}/retry")
    public ApiResponse<String> retryEvent(@PathVariable String id) {
        FailedSyncEvent event = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Event"));

        if ("RESOLVED".equals(event.getStatus())) {
            return ApiResponse.success("Event này đã được xử lý thành công trước đó.");
        }

        // Bắn lại vào Exchange gốc
        rabbitTemplate.convertAndSend(
                event.getExchangeName(),
                event.getRoutingKey(),
                event.getPayload() // Đẩy lại nguyên bản JSON gốc
        );

        event.setStatus("RESOLVED");
        repository.save(event);

        return ApiResponse.success("Đã đưa message trở lại hàng đợi gốc.");
    }
}