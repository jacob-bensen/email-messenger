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

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM ApiSendRequest r WHERE r.userId = :userId AND r.createdAt < :cutoff")
    int deleteByUserIdCreatedBefore(@Param("userId") long userId, @Param("cutoff") LocalDateTime cutoff);
}
