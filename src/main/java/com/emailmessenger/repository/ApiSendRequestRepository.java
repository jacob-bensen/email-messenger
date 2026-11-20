package com.emailmessenger.repository;

import com.emailmessenger.domain.ApiSendRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ApiSendRequestRepository extends JpaRepository<ApiSendRequest, Long> {

    @Query("SELECT r FROM ApiSendRequest r WHERE r.user.id = :userId AND r.idempotencyKey = :key")
    Optional<ApiSendRequest> findByUserIdAndKey(@Param("userId") long userId, @Param("key") String key);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM ApiSendRequest r WHERE r.user.id = :userId AND r.idempotencyKey = :key")
    int deleteByUserIdAndKey(@Param("userId") long userId, @Param("key") String key);
}
