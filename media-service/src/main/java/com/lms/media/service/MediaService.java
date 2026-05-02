package com.lms.media.service;

import com.lms.media.dto.request.GetUploadUrlRequest;
import com.lms.media.dto.response.GetUploadUrlResponse;

public interface MediaService {

    GetUploadUrlResponse requestUploadUrl (GetUploadUrlRequest request);
}
