package com.lms.notification.event.payload;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CourseStatusChangedEvent {
    String eventId;
    String courseId;
    String courseTitle;
    String instructorId;
    String status; // "APPROVED" hoặc "REJECTED"
    String rejectionNote; // Lý do từ chối (nếu có)
}
