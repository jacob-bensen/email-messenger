package com.emailmessenger.repository;

import com.emailmessenger.domain.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {

    Optional<Message> findByMessageIdHeader(String messageIdHeader);

    @Query("SELECT m FROM Message m WHERE m.thread.id = :threadId ORDER BY m.sentAt ASC")
    List<Message> findByThreadIdOrderBySentAtAsc(@Param("threadId") Long threadId);
}
