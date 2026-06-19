package com.emailmessenger.repository;

import com.emailmessenger.domain.MailAccount;
import com.emailmessenger.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MailAccountRepository extends JpaRepository<MailAccount, Long> {

    List<MailAccount> findByUserOrderByCreatedAtAsc(User user);

    long countByUser(User user);

    Optional<MailAccount> findByUserAndHostAndUsername(User user, String host, String username);

    Optional<MailAccount> findByIdAndUser(Long id, User user);

    /**
     * Onboarding-funnel slice: how many users in the supplied cohort have
     * connected at least one mailbox. Distinct on the user side so a user
     * with multiple mailboxes still counts once. Empty cohort returns 0.
     */
    @Query("SELECT COUNT(DISTINCT a.user.id) FROM MailAccount a WHERE a.user.id IN :userIds")
    long countDistinctOwnersIn(@Param("userIds") Collection<Long> userIds);

    /**
     * Accounts the recurring scheduler should poll on this tick: not
     * circuit-broken and either never scheduled (fresh connect) or whose
     * stamped {@code nextPollAt} has now passed. Manual "Sync now" goes
     * through {@code findById} and bypasses this filter on purpose.
     */
    @Query("""
            select a from MailAccount a
            where a.pollingSuspended = false
              and (a.nextPollAt is null or a.nextPollAt <= :now)
            """)
    List<MailAccount> findDueForPolling(@Param("now") LocalDateTime now);

    /**
     * The current user's accounts that are due for a poll right now — used by
     * the on-inbox-open refresh so opening /threads pulls fresh mail
     * immediately, without re-polling accounts the scheduler already serviced.
     */
    @Query("""
            select a from MailAccount a
            where a.user.id = :userId
              and a.pollingSuspended = false
              and (a.nextPollAt is null or a.nextPollAt <= :now)
            """)
    List<MailAccount> findDueForPollingByUserId(@Param("userId") Long userId,
                                                @Param("now") LocalDateTime now);
}
