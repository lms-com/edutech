package com.lms.iam.repository;

import com.lms.iam.model.LearnerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LearnerProfileRepository extends JpaRepository<LearnerProfile, String> {

    Optional<LearnerProfile> findLearnerProfileByUserId(String userId);
}
