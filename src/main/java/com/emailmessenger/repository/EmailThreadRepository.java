package com.emailmessenger.repository;

import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

public interface EmailThreadRepository extends JpaRepository<EmailThread, Long> {

    Optional<EmailThread> findByRootMessageIdAndOwner(String rootMessageId, User owner);

    Page<EmailThread> findByOwnerOrderByUpdatedAtDesc(User owner, Pageable pageable);

    Optional<EmailThread> findByIdAndOwner(Long id, User owner);
}
