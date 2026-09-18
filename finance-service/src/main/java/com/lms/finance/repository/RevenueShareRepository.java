package com.lms.finance.repository;

import com.lms.finance.entity.RevenueShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RevenueShareRepository extends JpaRepository<RevenueShare, String> {

}
