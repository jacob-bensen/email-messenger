package com.emailmessenger.repository;

import com.emailmessenger.domain.AuthEvent;
import com.emailmessenger.domain.AuthEventType;
import com.emailmessenger.domain.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AuthEventRepository extends JpaRepository<AuthEvent, Long> {

    // id is the tie-breaker: events recorded in the same clock instant (a tight
    // burst on a fast box) would otherwise order non-deterministically, since
    // createdAt alone isn't unique. id DESC keeps them in insertion order.
    List<AuthEvent> findByUserOrderByCreatedAtDescIdDesc(User user, Pageable pageable);

    @Query("""
            SELECT COUNT(e) FROM AuthEvent e
            WHERE e.eventType = :type
              AND e.email = :email
              AND e.createdAt >= :since
            """)
    long countRecentForEmail(@Param("type") AuthEventType type,
                             @Param("email") String email,
                             @Param("since") LocalDateTime since);

    @Query("""
            SELECT COUNT(e) FROM AuthEvent e
            WHERE e.eventType = :type
              AND e.ipAddress = :ip
              AND e.createdAt >= :since
            """)
    long countRecentForIp(@Param("type") AuthEventType type,
                          @Param("ip") String ip,
                          @Param("since") LocalDateTime since);
}
