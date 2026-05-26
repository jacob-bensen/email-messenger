package com.emailmessenger.repository;

import com.emailmessenger.domain.MailAccount;
import com.emailmessenger.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MailAccountRepository extends JpaRepository<MailAccount, Long> {

    List<MailAccount> findByUserOrderByCreatedAtAsc(User user);

    long countByUser(User user);

    Optional<MailAccount> findByUserAndHostAndUsername(User user, String host, String username);

    Optional<MailAccount> findByIdAndUser(Long id, User user);

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
}
