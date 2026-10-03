package com.lms.iam.repository;

import com.lms.iam.model.InstructorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InstructorProfileRepository extends JpaRepository<InstructorProfile, String> {
    Optional<InstructorProfile> findInstructorProfileByUserId(String userId);
}
