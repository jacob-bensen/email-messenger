package com.emailmessenger.repository;

import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

public interface EmailThreadRepository extends JpaRepository<EmailThread, Long> {

    Optional<EmailThread> findByRootMessageIdAndOwner(String rootMessageId, User owner);

    Page<EmailThread> findByOwnerOrderByUpdatedAtDesc(User owner, Pageable pageable);

    Optional<EmailThread> findByIdAndOwner(Long id, User owner);

    long countByOwner(User owner);

    @Query("""
            SELECT DISTINCT t FROM EmailThread t
            WHERE t.owner = :owner
              AND (
                LOWER(t.subject) LIKE LOWER(CONCAT('%', :q, '%'))
                OR EXISTS (
                  SELECT 1 FROM Message m
                  WHERE m.thread = t
                    AND (
                      LOWER(m.sender.email) LIKE LOWER(CONCAT('%', :q, '%'))
                      OR LOWER(COALESCE(m.sender.displayName, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                    )
                )
              )
            ORDER BY t.updatedAt DESC
            """)
    Page<EmailThread> search(@Param("owner") User owner,
                             @Param("q") String query,
                             Pageable pageable);
}
