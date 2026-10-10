package com.lms.finance.repository;

import com.lms.finance.entity.RevenueShare;
import com.lms.finance.enums.RevenueShareStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RevenueShareRepository extends JpaRepository<RevenueShare, String> {
    List<RevenueShare> findByStatusAndReleaseAtLessThanEqual(RevenueShareStatus status, Instant currentTime);

    List<RevenueShare> findByInstructorIdAndCreatedAtGreaterThanEqualOrderByCreatedAtAsc(String instructorId, Instant since);

    @Query("SELECT COALESCE(SUM(rs.instructorAmount), 0) FROM RevenueShare rs WHERE rs.instructorId = :instructorId")
    BigDecimal sumInstructorAmountByInstructorId(@Param("instructorId") String instructorId);

    @Query("SELECT COALESCE(SUM(rs.instructorAmount), 0) FROM RevenueShare rs WHERE rs.instructorId = :instructorId AND rs.createdAt >= :start AND rs.createdAt < :end")
    BigDecimal sumInstructorAmountByInstructorIdAndCreatedAtBetween(
            @Param("instructorId") String instructorId,
            @Param("start") Instant start,
            @Param("end") Instant end);

    @Query("SELECT COUNT(rs) FROM RevenueShare rs WHERE rs.instructorId = :instructorId AND rs.createdAt >= :start AND rs.createdAt < :end")
    long countByInstructorIdAndCreatedAtBetween(
            @Param("instructorId") String instructorId,
            @Param("start") Instant start,
            @Param("end") Instant end);

    @Query("SELECT COUNT(DISTINCT rs.orderId) FROM RevenueShare rs WHERE rs.instructorId = :instructorId")
    long countDistinctOrdersByInstructorId(@Param("instructorId") String instructorId);
}
