package com.emailmessenger.repository;

import com.emailmessenger.domain.ApiSendRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ApiSendRequestRepository extends JpaRepository<ApiSendRequest, Long> {

    Optional<ApiSendRequest> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    /** Takes over a pending claim that is still stale, so two concurrent retries cannot both win. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE ApiSendRequest r SET r.claimedAt = :now
            WHERE r.id = :id AND r.claimedAt < :staleBefore AND r.completedAt IS NULL
            """)
    int reclaimStale(@Param("id") long id, @Param("staleBefore") LocalDateTime staleBefore,
                     @Param("now") LocalDateTime now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM ApiSendRequest r WHERE r.id = :id AND r.completedAt IS NULL")
    int deletePending(@Param("id") long id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM ApiSendRequest r WHERE r.claimedAt < :cutoff")
    int deleteClaimedBefore(@Param("cutoff") LocalDateTime cutoff);
}
