package com.emailmessenger.repository;

import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.domain.ThreadNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ThreadNoteRepository extends JpaRepository<ThreadNote, Long> {

    List<ThreadNote> findByThreadOrderByCreatedAtAsc(EmailThread thread);

    long countByThread(EmailThread thread);
}
