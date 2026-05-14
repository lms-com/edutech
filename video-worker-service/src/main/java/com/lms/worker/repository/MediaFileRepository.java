package com.lms.worker.repository;

import com.lms.worker.model.MediaFile;
import com.lms.worker.model.MediaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, String> {

    @Modifying
    @Query("""
            DELETE from MediaFile mf
            WHERE mf.status = :status AND mf.updatedAt < :threshold
""")
    void deleteByStatusAndUpdatedAtBefore (MediaStatus status, Instant threshold);
}
