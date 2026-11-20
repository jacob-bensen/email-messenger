package com.emailmessenger.repository;

import com.emailmessenger.domain.ApiSendRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApiSendRequestRepository extends JpaRepository<ApiSendRequest, Long> {

    Optional<ApiSendRequest> findByUserIdAndIdempotencyKey(long userId, String idempotencyKey);
}
