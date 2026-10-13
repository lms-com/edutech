package com.lms.iam.service;

import com.lms.iam.dto.request.ReviewInstructorApplicationRequest;
import com.lms.iam.dto.request.SubmitInstructorApplicationRequest;
import com.lms.iam.dto.response.InstructorApplicationResponse;
import com.lms.iam.model.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface InstructorApplicationService {

    InstructorApplicationResponse submitApplication(String userId, SubmitInstructorApplicationRequest request);

    List<InstructorApplicationResponse> getMyApplications(String userId);

    Page<InstructorApplicationResponse> getApplicationsForAdmin(ApplicationStatus status, Pageable pageable);

    InstructorApplicationResponse reviewApplication(String adminId, String applicationId, ReviewInstructorApplicationRequest request);
}
