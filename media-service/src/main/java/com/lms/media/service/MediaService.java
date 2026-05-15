package com.lms.media.service;

import com.lms.media.dto.request.GetUploadUrlRequest;
import com.lms.media.dto.response.GetUploadUrlResponse;
import com.lms.media.model.MediaFile;

import java.util.List;

public interface MediaService {

    GetUploadUrlResponse requestUploadUrl (GetUploadUrlRequest request);

    void confirmUploadUrl (String mediaId);

    String getDisplayUrl (String mediaId, String deviceFingerPrint);

    void autoCleanPendingFilesAfter12Hours ();

    void removeFile (String mediaId);


    /**
     * Cac Service chi danh cho Admin
     */

    List<MediaFile> getAllMediaFiles();
}
