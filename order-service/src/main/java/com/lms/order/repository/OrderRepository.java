package com.lms.order.repository;

import com.lms.order.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    Page<Order> findByLearnerIdOrderByCreatedAtDesc(String learnerId, Pageable pageable);
    Optional<Order> findByIdAndLearnerId(String id, String learnerId);
}
