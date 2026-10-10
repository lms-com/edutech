package com.lms.media.dto.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoProcessMessage {
    private String mediaId;
    private String storedFileName;
}
