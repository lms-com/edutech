package com.lms.notification.event.payload;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

/**
 * Bản sao contract của OrderCompletedMessage bên order-service.
 *
 * order.exchange / order.completed chỉ mang orderId, learnerId và items. Email
 * học viên không nằm trong bản tin nên phải tra qua iam-service, còn tổng tiền
 * và danh sách khóa học được suy ra từ items.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SendOrderCompletedEvent {
    String orderId;
    String learnerId;
    List<OrderItem> items;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class OrderItem {
        String courseId;
        String instructorId;
        BigDecimal finalPrice;
        String paymentCurrency;
        BigDecimal commissionRate;
    }
}
