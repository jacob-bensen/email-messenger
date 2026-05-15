package com.emailmessenger.repository;

import com.emailmessenger.domain.Message;
import com.emailmessenger.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT m FROM Message m WHERE m.messageIdHeader = :header AND m.thread.owner = :owner")
    Optional<Message> findByMessageIdHeaderAndOwner(@Param("header") String header,
                                                   @Param("owner") User owner);

    @Query("SELECT m FROM Message m WHERE m.thread.id = :threadId ORDER BY m.sentAt ASC")
    List<Message> findByThreadIdOrderBySentAtAsc(@Param("threadId") Long threadId);
}
