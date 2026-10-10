package com.lms.worker.dto.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoCompletedMessage {
    private String mediaId;  // Tuong ung videoUrl bên course-service
}
