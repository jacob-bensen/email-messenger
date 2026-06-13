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

    Optional<User> findByGoogleSubject(String googleSubject);

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

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE User u SET u.lastActivationNudgeSentAt = :ts WHERE u.id = :id")
    int touchActivationNudgeSent(@Param("id") Long id, @Param("ts") LocalDateTime ts);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE User u SET u.lastActivationFollowupSentAt = :ts WHERE u.id = :id")
    int touchActivationFollowupSent(@Param("id") Long id, @Param("ts") LocalDateTime ts);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE User u SET u.lastActivationLastChanceSentAt = :ts WHERE u.id = :id")
    int touchActivationLastChanceSent(@Param("id") Long id, @Param("ts") LocalDateTime ts);

    // Signups that haven't crossed the IMAP-credentials chasm: enabled,
    // signed up at least the cool-off window ago, never previously nudged,
    // and no mail_account row at all. The activation service still
    // post-filters by opt-out so a single unsubscribe link kills every
    // automated marketing email; cheap full-scan, no index needed at our
    // scale because the candidate set is bounded by daily signups.
    @Query("""
            SELECT u FROM User u
            WHERE u.enabled = true
              AND u.createdAt < :cutoff
              AND u.lastActivationNudgeSentAt IS NULL
              AND NOT EXISTS (
                  SELECT 1 FROM MailAccount a WHERE a.user = u
              )
            """)
    List<User> findActivationCandidates(@Param("cutoff") LocalDateTime cutoff);

    // Day-3 follow-up cohort: signups that cleared the 72h window, already
    // received the day-1 nudge, never got the follow-up, and still have
    // no mail_account row. Sequencing on `lastActivationNudgeSentAt IS NOT
    // NULL` keeps the drip in order — a user can't receive day-3 before
    // day-1 even if the day-1 scheduler skipped a tick or was wired up
    // after they crossed the 72h threshold.
    @Query("""
            SELECT u FROM User u
            WHERE u.enabled = true
              AND u.createdAt < :cutoff
              AND u.lastActivationNudgeSentAt IS NOT NULL
              AND u.lastActivationFollowupSentAt IS NULL
              AND NOT EXISTS (
                  SELECT 1 FROM MailAccount a WHERE a.user = u
              )
            """)
    List<User> findActivationFollowupCandidates(@Param("cutoff") LocalDateTime cutoff);

    // Day-7 last-chance cohort: signups a full week old, already received
    // both day-1 and day-3, never got the last-chance, still no
    // mail_account row. Sequencing on `lastActivationFollowupSentAt IS
    // NOT NULL` (transitively requires day-1 too — day-3 won't fire
    // without day-1) keeps the drip in order so this is the third and
    // final touch in the sequence.
    @Query("""
            SELECT u FROM User u
            WHERE u.enabled = true
              AND u.createdAt < :cutoff
              AND u.lastActivationFollowupSentAt IS NOT NULL
              AND u.lastActivationLastChanceSentAt IS NULL
              AND NOT EXISTS (
                  SELECT 1 FROM MailAccount a WHERE a.user = u
              )
            """)
    List<User> findActivationLastChanceCandidates(@Param("cutoff") LocalDateTime cutoff);

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
