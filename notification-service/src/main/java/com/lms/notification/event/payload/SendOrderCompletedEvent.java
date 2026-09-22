package com.lms.notification.event.payload;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SendOrderCompletedEvent {
    String eventId;
    String orderId;
    String learnerId;
    String learnerEmail; // Lựa chọn A: Publisher đính kèm sẵn email để tối ưu tốc độ
    Long totalAmount; // Số tiền đơn hàng để render biên lai hóa đơn
    List<String> courseIds; // Danh sách mã khóa học học viên đã mua
}