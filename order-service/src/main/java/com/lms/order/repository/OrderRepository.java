package com.lms.order.repository;

import com.lms.order.model.Order;
import com.lms.order.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    Page<Order> findByLearnerIdOrderByCreatedAtDesc(String learnerId, Pageable pageable);
    Optional<Order> findByIdAndLearnerId(String id, String learnerId);
    List<Order> findByLearnerIdAndStatus(String learnerId, OrderStatus status);
    List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, Instant expiryTime);
}
