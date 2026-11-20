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

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM ApiSendRequest r WHERE r.createdAt < :cutoff")
    int deleteCreatedBefore(@Param("cutoff") LocalDateTime cutoff);
}
