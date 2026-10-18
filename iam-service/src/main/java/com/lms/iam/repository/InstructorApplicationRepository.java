package com.lms.iam.repository;

import com.lms.iam.model.ApplicationStatus;
import com.lms.iam.model.InstructorApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InstructorApplicationRepository extends JpaRepository<InstructorApplication, String> {

    List<InstructorApplication> findByUserIdOrderByCreatedAtDesc(String userId);

    Optional<InstructorApplication> findFirstByUserIdAndStatus(String userId, ApplicationStatus status);

    Page<InstructorApplication> findByStatus(ApplicationStatus status, Pageable pageable);
}
