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

    /**
     * Notes posted strictly inside [start, end). Drives the
     * "prior 30 days" baseline column on the Team-plan adoption card so
     * the operator can see the current window's lift against the window
     * before it, instead of staring at a number with no point of
     * reference.
     */
    @Query("""
            SELECT n FROM ThreadNote n JOIN FETCH n.team
            WHERE n.createdAt >= :start AND n.createdAt < :end
            """)
    List<ThreadNote> findCreatedBetween(@Param("start") LocalDateTime start,
                                        @Param("end") LocalDateTime end);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
