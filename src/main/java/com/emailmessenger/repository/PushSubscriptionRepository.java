package com.emailmessenger.repository;

import com.emailmessenger.domain.PushSubscription;
import com.emailmessenger.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {

    List<PushSubscription> findByUserOrderByCreatedAtDesc(User user);

    Optional<PushSubscription> findByUserAndEndpoint(User user, String endpoint);

    long countByUser(User user);

    @Modifying
    @Query("delete from PushSubscription s where s.user = :user and s.endpoint = :endpoint")
    int deleteByUserAndEndpoint(@Param("user") User user, @Param("endpoint") String endpoint);
}
