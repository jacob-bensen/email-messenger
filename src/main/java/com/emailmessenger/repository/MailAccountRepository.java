package com.emailmessenger.repository;

import com.emailmessenger.domain.MailAccount;
import com.emailmessenger.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MailAccountRepository extends JpaRepository<MailAccount, Long> {

    List<MailAccount> findByUserOrderByCreatedAtAsc(User user);

    long countByUser(User user);

    Optional<MailAccount> findByUserAndHostAndUsername(User user, String host, String username);

    Optional<MailAccount> findByIdAndUser(Long id, User user);
}
