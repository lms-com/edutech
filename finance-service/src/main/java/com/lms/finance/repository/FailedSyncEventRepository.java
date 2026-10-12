package com.lms.finance.repository;

import com.lms.finance.entity.FailedSyncEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FailedSyncEventRepository extends JpaRepository<FailedSyncEvent,String> {
}
