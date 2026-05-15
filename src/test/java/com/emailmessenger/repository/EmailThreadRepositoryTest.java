package com.emailmessenger.repository;

import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.domain.Message;
import com.emailmessenger.domain.Participant;
import com.emailmessenger.domain.RecipientType;
import com.emailmessenger.domain.User;
import org.junit.jupiter.api.BeforeEach;
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
    @Autowired UserRepository userRepo;

    private User owner;

    @BeforeEach
    void setUp() {
        owner = userRepo.save(new User("owner@example.com", "hash", "Owner"));
    }

    @Test
    void savesThreadAndFindsById() {
        var thread = new EmailThread(owner, "Re: Project update", "<root-msg-id@example.com>");
        var saved = threadRepo.save(thread);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        Optional<EmailThread> found = threadRepo.findByRootMessageIdAndOwner(
                "<root-msg-id@example.com>", owner);
        assertThat(found).isPresent();
        assertThat(found.get().getSubject()).isEqualTo("Re: Project update");
    }

    @Test
    void threadsOrderedByUpdatedAtDesc() {
        threadRepo.save(new EmailThread(owner, "Thread A", "<a@example.com>"));
        threadRepo.save(new EmailThread(owner, "Thread B", "<b@example.com>"));

        var page = threadRepo.findByOwnerOrderByUpdatedAtDesc(owner, PageRequest.of(0, 10));
        assertThat(page.getContent()).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void threadsScopedByOwner() {
        var alice = userRepo.save(new User("alice@example.com", "h", "Alice"));
        var bob = userRepo.save(new User("bob@example.com", "h", "Bob"));
        threadRepo.save(new EmailThread(alice, "Alice thread", "<alice@example.com>"));
        threadRepo.save(new EmailThread(bob, "Bob thread", "<bob@example.com>"));

        var aliceThreads = threadRepo.findByOwnerOrderByUpdatedAtDesc(alice, PageRequest.of(0, 10));
        assertThat(aliceThreads.getContent()).hasSize(1);
        assertThat(aliceThreads.getContent().get(0).getSubject()).isEqualTo("Alice thread");
    }

    @Test
    void findByIdAndOwnerEnforcesIsolation() {
        var alice = userRepo.save(new User("alice2@example.com", "h", null));
        var bob = userRepo.save(new User("bob2@example.com", "h", null));
        var aliceThread = threadRepo.save(new EmailThread(alice, "Alice's", "<a2@example.com>"));

        assertThat(threadRepo.findByIdAndOwner(aliceThread.getId(), alice)).isPresent();
        assertThat(threadRepo.findByIdAndOwner(aliceThread.getId(), bob)).isEmpty();
    }

    @Test
    void messagesPersistWithThread() {
        var sender = participantRepo.save(new Participant("sender@example.com", "Sender"));
        var thread = threadRepo.save(new EmailThread(owner, "Hello", "<hello@example.com>"));

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
    void findsByMessageIdHeaderAndOwner() {
        var sender = participantRepo.save(new Participant("find@example.com", "Find Test"));
        var thread = threadRepo.save(new EmailThread(owner, "Find test", "<find@example.com>"));

        var msg = new Message(thread, sender, "Find", "body", null, LocalDateTime.now());
        msg.setMessageIdHeader("<unique-msg-id@example.com>");
        messageRepo.save(msg);

        assertThat(messageRepo.findByMessageIdHeaderAndOwner("<unique-msg-id@example.com>", owner))
                .isPresent();
        assertThat(messageRepo.findByMessageIdHeaderAndOwner("<nonexistent@example.com>", owner))
                .isEmpty();
    }
}
