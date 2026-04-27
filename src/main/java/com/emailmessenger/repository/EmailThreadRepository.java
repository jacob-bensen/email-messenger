package com.emailmessenger.repository;

import com.emailmessenger.domain.EmailThread;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

public interface EmailThreadRepository extends JpaRepository<EmailThread, Long> {

    Optional<EmailThread> findByRootMessageId(String rootMessageId);

    Page<EmailThread> findAllByOrderByUpdatedAtDesc(Pageable pageable);

    // Loads messages and their senders in one JOIN query, eliminating N+1 on the conversation view.
    @Query("SELECT DISTINCT t FROM EmailThread t " +
           "LEFT JOIN FETCH t.messages m " +
           "LEFT JOIN FETCH m.sender " +
           "WHERE t.id = :id")
    Optional<EmailThread> findByIdWithMessages(@Param("id") Long id);
}
