package com.lms.order.repository;

import com.lms.order.model.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PromotionRepository extends JpaRepository<Promotion, String> {

    Optional<Promotion> findByPromotionCode(String promotionCode);
}
