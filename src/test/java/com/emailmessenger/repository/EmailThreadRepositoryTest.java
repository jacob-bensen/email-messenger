package com.emailmessenger.repository;

import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.domain.Message;
import com.emailmessenger.domain.Participant;
import com.emailmessenger.domain.RecipientType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class EmailThreadRepositoryTest {

    @Autowired EmailThreadRepository threadRepo;
    @Autowired MessageRepository messageRepo;
    @Autowired ParticipantRepository participantRepo;

    @Test
    void savesThreadAndFindsById() {
        var thread = new EmailThread("Re: Project update", "<root-msg-id@example.com>");
        var saved = threadRepo.save(thread);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        Optional<EmailThread> found = threadRepo.findByRootMessageId("<root-msg-id@example.com>");
        assertThat(found).isPresent();
        assertThat(found.get().getSubject()).isEqualTo("Re: Project update");
    }

    @Test
    void threadsOrderedByUpdatedAtDesc() {
        var t1 = threadRepo.save(new EmailThread("Thread A", "<a@example.com>"));
        var t2 = threadRepo.save(new EmailThread("Thread B", "<b@example.com>"));

        var page = threadRepo.findAllByOrderByUpdatedAtDesc(PageRequest.of(0, 10));
        assertThat(page.getContent()).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void messagesPersistWithThread() {
        var sender = participantRepo.save(new Participant("sender@example.com", "Sender"));
        var thread = threadRepo.save(new EmailThread("Hello", "<hello@example.com>"));

        var msg = new Message(thread, sender, "Hello",
                "Plain body text", "<p>HTML body</p>", LocalDateTime.now());
        msg.setMessageIdHeader("<hello@example.com>");
        msg.addRecipient(sender, RecipientType.TO);
        messageRepo.save(msg);

        var messages = messageRepo.findByThreadIdOrderBySentAtAsc(thread.getId());
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getBodyPlain()).isEqualTo("Plain body text");
    }

    @Test
    void findsByMessageIdHeader() {
        var sender = participantRepo.save(new Participant("find@example.com", "Find Test"));
        var thread = threadRepo.save(new EmailThread("Find test", "<find@example.com>"));

        var msg = new Message(thread, sender, "Find", "body", null, LocalDateTime.now());
        msg.setMessageIdHeader("<unique-msg-id@example.com>");
        messageRepo.save(msg);

        assertThat(messageRepo.findByMessageIdHeader("<unique-msg-id@example.com>")).isPresent();
        assertThat(messageRepo.findByMessageIdHeader("<nonexistent@example.com>")).isEmpty();
    }
}
