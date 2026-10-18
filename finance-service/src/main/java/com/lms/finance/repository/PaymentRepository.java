package com.lms.finance.repository;

import com.lms.finance.entity.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {
    @Query(value = """
        SELECT COALESCE (
                   CASE 
                       WHEN SUM(CASE WHEN status IN ('SUCCESS', 'FAILED', 'REFUNDED') THEN 1 ELSE 0 END) > 0 THEN -1
                    ELSE SUM(CASE WHEN status = 'PROCESSING' THEN 1 ELSE 0 END)
                   END,
                   0
            ) AS result
        FROM payments
        WHERE order_id = :orderId
    """, nativeQuery = true)
    int countByOrderIdNotPAID(@Param("orderId") String orderId);

    boolean existsPaymentByPaymentRef(String paymentRef);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Payment> findPaymentByPaymentRef(String paymentRef);
}
