package com.lms.finance.repository;

import com.lms.finance.entity.InstructorBalance;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface InstructorBalanceRepository extends JpaRepository<InstructorBalance, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<InstructorBalance> findById(@Param("id") String id);

    boolean existsByInstructorId(String instructorId);

    @Query(value = """
        SELECT * FROM instructor_balances
        WHERE instructor_id = :instructorId
    """, nativeQuery = true)
    Optional<InstructorBalance> findByInstructorId(@Param("instructorId") String instructorId);

    List<InstructorBalance> findByInstructorIdIn(Collection<String> instructorIds);
}