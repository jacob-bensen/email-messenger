package com.emailmessenger.repository;

import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.domain.ThreadNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ThreadNoteRepository extends JpaRepository<ThreadNote, Long> {

    List<ThreadNote> findByThreadOrderByCreatedAtAsc(EmailThread thread);

    long countByThread(EmailThread thread);

    /**
     * Notes posted inside the rolling window. Eager-loads the team so the
     * adoption-metrics service can count distinct teams without an N+1.
     */
    @Query("""
            SELECT n FROM ThreadNote n JOIN FETCH n.team
            WHERE n.createdAt >= :since
            """)
    List<ThreadNote> findCreatedSince(@Param("since") LocalDateTime since);

    long countByCreatedAtAfter(LocalDateTime since);
}
