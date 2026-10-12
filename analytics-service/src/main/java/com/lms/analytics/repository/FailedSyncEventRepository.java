package com.lms.analytics.repository;

import com.lms.analytics.model.FailedSyncEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FailedSyncEventRepository extends JpaRepository<FailedSyncEvent,String> {
}
