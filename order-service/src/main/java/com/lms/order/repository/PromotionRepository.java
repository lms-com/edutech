package com.lms.order.repository;

import com.lms.order.model.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, String> {

    Optional<Promotion> findByCode(String promotionCode);

    @Query("""
        SELECT p FROM Promotion p
        WHERE p.id IN :promotionIds
    """)
    List<Promotion> findAllByIds (@Param("promotionIds") List<String> promotionIds);
}
