package com.lms.finance.repository;

import com.lms.finance.entity.PayoutRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PayoutRequestRepository extends JpaRepository<PayoutRequest, String>, JpaSpecificationExecutor<PayoutRequest> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PayoutRequest> findById(@Param("payoutRequestId") String payoutRequestId);

    Page<PayoutRequest> findAll (Specification<PayoutRequest> spec, Pageable pageable);
}
