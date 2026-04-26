package com.emailmessenger.repository;

import com.emailmessenger.domain.EmailThread;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

public interface EmailThreadRepository extends JpaRepository<EmailThread, Long> {

    Optional<EmailThread> findByRootMessageId(String rootMessageId);

    Page<EmailThread> findAllByOrderByUpdatedAtDesc(Pageable pageable);
}
