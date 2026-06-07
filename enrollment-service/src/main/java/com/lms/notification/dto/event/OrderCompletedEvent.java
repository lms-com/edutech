package com.lms.notification.dto.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCompletedEvent {
    String eventId;
    String orderId;
    String learnerId;
    List<String> courseIds;  // 1 đơn có thể mua nhiều khóa
}
