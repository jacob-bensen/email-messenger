package com.emailmessenger.repository;

import com.emailmessenger.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE User u SET u.lastInboxVisitAt = :ts WHERE u.id = :id")
    int touchInboxVisit(@Param("id") Long id, @Param("ts") LocalDateTime ts);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE User u SET u.lastLoginAt = :ts WHERE LOWER(u.email) = LOWER(:email)")
    int touchLogin(@Param("email") String email, @Param("ts") LocalDateTime ts);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE User u SET u.lastReengagementSentAt = :ts WHERE u.id = :id")
    int touchReengagementSent(@Param("id") Long id, @Param("ts") LocalDateTime ts);

    // Users whose most recent observable activity (login OR inbox visit, with
    // created_at as the fallback for pre-tracking rows) is older than the
    // cutoff. The reengagement service post-filters by unread count, opt-out,
    // and idempotency, so this query stays a cheap full-scan over the users
    // table.
    @Query("""
            SELECT u FROM User u
            WHERE u.enabled = true
              AND COALESCE(u.lastInboxVisitAt, u.createdAt) < :cutoff
              AND COALESCE(u.lastLoginAt, u.createdAt) < :cutoff
            """)
    List<User> findDormantSince(@Param("cutoff") LocalDateTime cutoff);

    // Signup cohort feeding the operator funnel dashboard. acquisition_source
    // is the only field consumed downstream — keeping the projection narrow
    // avoids dragging the full User graph through the metrics service.
    @Query("SELECT u FROM User u WHERE u.createdAt >= :cutoff")
    List<User> findCreatedAtAfter(@Param("cutoff") LocalDateTime cutoff);
}
