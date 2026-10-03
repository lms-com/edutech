package com.lms.media.service;

import com.lms.media.dto.request.GetUploadUrlRequest;
import com.lms.media.dto.response.GetUploadUrlResponse;
import com.lms.media.model.MediaFile;

import java.util.List;

public interface MediaService {

    /**
     * Cac Service cho Instructor
     */

    GetUploadUrlResponse requestUploadUrl (GetUploadUrlRequest request);

    void confirmUploadUrl (String mediaId);

    void removeFile (String mediaId);


    /**
     * Cac Service Learner
     */

    String getVideoManifest (String learnerId, String mediaId);

    byte[] getEncryptionKey (String learnerId, String mediaId, String sessionId);


    /**
     * Cac service cua System
     */
    void autoCleanPendingFilesAfter12Hours ();


    /**
     * Cac Service chi danh cho Admin
     */

    List<MediaFile> getAllMediaFiles();
}
