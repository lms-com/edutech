package com.lms.finance.repository;

import com.lms.finance.entity.BalanceHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BalanceHistoryRepository extends JpaRepository<BalanceHistory,String>, JpaSpecificationExecutor<BalanceHistory> {
    Page<BalanceHistory> findAll (Specification <BalanceHistory> spec, Pageable pageable);
}
