package com.lms.order.repository;

import com.lms.order.model.RefundRequest;
import com.lms.order.model.RefundStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefundRequestRepository extends JpaRepository<RefundRequest, String> {
    Optional<RefundRequest> findByOrderId(String orderId);
    Optional<RefundRequest> findByOrderIdAndStatus(String orderId, RefundStatus status);
    Page<RefundRequest> findByLearnerId(String learnerId, Pageable pageable);
    Page<RefundRequest> findByStatus(RefundStatus status, Pageable pageable);
    Page<RefundRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
