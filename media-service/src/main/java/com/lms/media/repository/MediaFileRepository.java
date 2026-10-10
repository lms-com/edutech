package com.lms.media.repository;

import com.lms.media.model.MediaFile;
import com.lms.media.model.MediaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;

@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, String> {

    @Modifying
    @Query("""
            DELETE from MediaFile mf
            WHERE mf.status = :status AND mf.updatedAt < :threshold
""")
    void deleteByStatusAndUpdatedAtBefore (MediaStatus status, Instant threshold);
}
