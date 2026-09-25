package com.lms.finance.repository;

import com.lms.finance.entity.RevenueShare;
import com.lms.finance.enums.RevenueShareStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RevenueShareRepository extends JpaRepository<RevenueShare, String> {
    List<RevenueShare> findByStatusAndReleaseAtLessThanEqual(RevenueShareStatus status, Instant currentTime);
}
