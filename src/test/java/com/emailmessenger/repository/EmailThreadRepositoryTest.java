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

    @Test
    void searchMatchesSubjectCaseInsensitively() {
        threadRepo.save(new EmailThread(owner, "Quarterly Planning Doc", "<q1@example.com>"));
        threadRepo.save(new EmailThread(owner, "Birthday party", "<b1@example.com>"));

        var page = threadRepo.search(owner, "planning", PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getSubject()).isEqualTo("Quarterly Planning Doc");
    }

    @Test
    void searchMatchesParticipantDisplayName() {
        var ada = participantRepo.save(new Participant("ada@acme.com", "Ada Lovelace"));
        var grace = participantRepo.save(new Participant("grace@navy.mil", "Grace Hopper"));
        var t1 = threadRepo.save(new EmailThread(owner, "Project Athena", "<a@x>"));
        var t2 = threadRepo.save(new EmailThread(owner, "Project Olympus", "<o@x>"));
        messageRepo.save(new Message(t1, ada, "Project Athena", "body", null, LocalDateTime.now()));
        messageRepo.save(new Message(t2, grace, "Project Olympus", "body", null, LocalDateTime.now()));

        var page = threadRepo.search(owner, "lovelace", PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(EmailThread::getSubject)
                .containsExactly("Project Athena");
    }

    @Test
    void searchMatchesParticipantEmail() {
        var alex = participantRepo.save(new Participant("alex@acme.com", "Alex"));
        var thread = threadRepo.save(new EmailThread(owner, "Invoice followup", "<inv@x>"));
        messageRepo.save(new Message(thread, alex, "Invoice", "body", null, LocalDateTime.now()));
        threadRepo.save(new EmailThread(owner, "Unrelated subject", "<u@x>"));

        var page = threadRepo.search(owner, "acme", PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(EmailThread::getSubject)
                .containsExactly("Invoice followup");
    }

    @Test
    void searchScopedByOwner() {
        var alice = userRepo.save(new User("alice-search@example.com", "h", "Alice"));
        var bob = userRepo.save(new User("bob-search@example.com", "h", "Bob"));
        threadRepo.save(new EmailThread(alice, "Shared subject", "<a-s@x>"));
        threadRepo.save(new EmailThread(bob, "Shared subject", "<b-s@x>"));

        var alicePage = threadRepo.search(alice, "shared", PageRequest.of(0, 10));
        var bobPage = threadRepo.search(bob, "shared", PageRequest.of(0, 10));

        assertThat(alicePage.getContent()).hasSize(1);
        assertThat(alicePage.getContent().get(0).getRootMessageId()).isEqualTo("<a-s@x>");
        assertThat(bobPage.getContent()).hasSize(1);
        assertThat(bobPage.getContent().get(0).getRootMessageId()).isEqualTo("<b-s@x>");
    }

    @Test
    void searchReturnsEmptyPageWhenNoMatch() {
        threadRepo.save(new EmailThread(owner, "Lunch tomorrow", "<l@x>"));

        var page = threadRepo.search(owner, "nonexistent-zzzz", PageRequest.of(0, 10));

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }

    @Test
    void searchDeduplicatesThreadsWithMultipleMatchingMessages() {
        var ada = participantRepo.save(new Participant("ada-dup@acme.com", "Ada Dup"));
        var thread = threadRepo.save(new EmailThread(owner, "Long thread", "<lt@x>"));
        messageRepo.save(new Message(thread, ada, "Reply 1", "body1", null, LocalDateTime.now()));
        messageRepo.save(new Message(thread, ada, "Reply 2", "body2", null, LocalDateTime.now()));

        var page = threadRepo.search(owner, "acme", PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
    }
}
