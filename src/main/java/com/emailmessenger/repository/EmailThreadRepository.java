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
                      OR LOWER(COALESCE(m.bodyPlain, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                    )
                )
              )
            ORDER BY t.updatedAt DESC
            """)
    Page<EmailThread> searchIncludingBody(@Param("owner") User owner,
                                          @Param("q") String query,
                                          Pageable pageable);

    // Did the query match any thread by body content that the subject/participant
    // search did NOT already surface? Drives the Free-tier upgrade nag.
    @Query("""
            SELECT (COUNT(t) > 0) FROM EmailThread t
            WHERE t.owner = :owner
              AND LOWER(t.subject) NOT LIKE LOWER(CONCAT('%', :q, '%'))
              AND NOT EXISTS (
                SELECT 1 FROM Message ms
                WHERE ms.thread = t
                  AND (
                    LOWER(ms.sender.email) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(COALESCE(ms.sender.displayName, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                  )
              )
              AND EXISTS (
                SELECT 1 FROM Message mb
                WHERE mb.thread = t
                  AND LOWER(COALESCE(mb.bodyPlain, '')) LIKE LOWER(CONCAT('%', :q, '%'))
              )
            """)
    boolean hasBodyOnlyMatch(@Param("owner") User owner, @Param("q") String query);
}
