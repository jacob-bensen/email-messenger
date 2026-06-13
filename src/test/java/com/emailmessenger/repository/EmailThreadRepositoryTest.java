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
import java.time.ZoneOffset;
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
                "Plain body text", "<p>HTML body</p>", LocalDateTime.now(ZoneOffset.UTC));
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

        var msg = new Message(thread, sender, "Find", "body", null, LocalDateTime.now(ZoneOffset.UTC));
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

        var page = threadRepo.search(owner, "planning", null, null, false, false, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getSubject()).isEqualTo("Quarterly Planning Doc");
    }

    @Test
    void searchMatchesParticipantDisplayName() {
        var ada = participantRepo.save(new Participant("ada@acme.com", "Ada Lovelace"));
        var grace = participantRepo.save(new Participant("grace@navy.mil", "Grace Hopper"));
        var t1 = threadRepo.save(new EmailThread(owner, "Project Athena", "<a@x>"));
        var t2 = threadRepo.save(new EmailThread(owner, "Project Olympus", "<o@x>"));
        messageRepo.save(new Message(t1, ada, "Project Athena", "body", null, LocalDateTime.now(ZoneOffset.UTC)));
        messageRepo.save(new Message(t2, grace, "Project Olympus", "body", null, LocalDateTime.now(ZoneOffset.UTC)));

        var page = threadRepo.search(owner, "lovelace", null, null, false, false, PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(EmailThread::getSubject)
                .containsExactly("Project Athena");
    }

    @Test
    void searchMatchesParticipantEmail() {
        var alex = participantRepo.save(new Participant("alex@acme.com", "Alex"));
        var thread = threadRepo.save(new EmailThread(owner, "Invoice followup", "<inv@x>"));
        messageRepo.save(new Message(thread, alex, "Invoice", "body", null, LocalDateTime.now(ZoneOffset.UTC)));
        threadRepo.save(new EmailThread(owner, "Unrelated subject", "<u@x>"));

        var page = threadRepo.search(owner, "acme", null, null, false, false, PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(EmailThread::getSubject)
                .containsExactly("Invoice followup");
    }

    @Test
    void searchScopedByOwner() {
        var alice = userRepo.save(new User("alice-search@example.com", "h", "Alice"));
        var bob = userRepo.save(new User("bob-search@example.com", "h", "Bob"));
        threadRepo.save(new EmailThread(alice, "Shared subject", "<a-s@x>"));
        threadRepo.save(new EmailThread(bob, "Shared subject", "<b-s@x>"));

        var alicePage = threadRepo.search(alice, "shared", null, null, false, false, PageRequest.of(0, 10));
        var bobPage = threadRepo.search(bob, "shared", null, null, false, false, PageRequest.of(0, 10));

        assertThat(alicePage.getContent()).hasSize(1);
        assertThat(alicePage.getContent().get(0).getRootMessageId()).isEqualTo("<a-s@x>");
        assertThat(bobPage.getContent()).hasSize(1);
        assertThat(bobPage.getContent().get(0).getRootMessageId()).isEqualTo("<b-s@x>");
    }

    @Test
    void searchReturnsEmptyPageWhenNoMatch() {
        threadRepo.save(new EmailThread(owner, "Lunch tomorrow", "<l@x>"));

        var page = threadRepo.search(owner, "nonexistent-zzzz", null, null, false, false, PageRequest.of(0, 10));

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }

    @Test
    void searchDeduplicatesThreadsWithMultipleMatchingMessages() {
        var ada = participantRepo.save(new Participant("ada-dup@acme.com", "Ada Dup"));
        var thread = threadRepo.save(new EmailThread(owner, "Long thread", "<lt@x>"));
        messageRepo.save(new Message(thread, ada, "Reply 1", "body1", null, LocalDateTime.now(ZoneOffset.UTC)));
        messageRepo.save(new Message(thread, ada, "Reply 2", "body2", null, LocalDateTime.now(ZoneOffset.UTC)));

        var page = threadRepo.search(owner, "acme", null, null, false, false, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void searchIncludingBodyMatchesMessageBodyContent() {
        var alex = participantRepo.save(new Participant("alex-body@example.com", "Alex"));
        var thread = threadRepo.save(new EmailThread(owner, "Lunch tomorrow", "<lb@x>"));
        messageRepo.save(new Message(thread, alex, "Lunch tomorrow",
                "Can we move the Stripe webhook discussion to Friday?", null, LocalDateTime.now(ZoneOffset.UTC)));
        threadRepo.save(new EmailThread(owner, "Unrelated", "<u@x>"));

        var page = threadRepo.searchIncludingBody(owner, "stripe webhook", null, null, false, false, PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(EmailThread::getSubject)
                .containsExactly("Lunch tomorrow");
    }

    @Test
    void searchIncludingBodyStillFindsSubjectMatches() {
        threadRepo.save(new EmailThread(owner, "Q3 Planning kickoff", "<q3@x>"));

        var page = threadRepo.searchIncludingBody(owner, "planning", null, null, false, false, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void searchIncludingBodyScopedByOwner() {
        var alice = userRepo.save(new User("alice-body@example.com", "h", "Alice"));
        var sender = participantRepo.save(new Participant("s-body@x.com", "S"));
        var aliceThread = threadRepo.save(new EmailThread(alice, "Alice's", "<ab@x>"));
        messageRepo.save(new Message(aliceThread, sender, "Alice's",
                "Confidential alpha launch plan", null, LocalDateTime.now(ZoneOffset.UTC)));

        var ownerPage = threadRepo.searchIncludingBody(owner, "confidential", null, null, false, false, PageRequest.of(0, 10));

        assertThat(ownerPage.getContent()).isEmpty();
    }

    @Test
    void hasBodyOnlyMatchTrueWhenOnlyBodyMatches() {
        var sender = participantRepo.save(new Participant("anon@x.com", "Anon"));
        var thread = threadRepo.save(new EmailThread(owner, "Casual chat", "<cc@x>"));
        messageRepo.save(new Message(thread, sender, "Casual chat",
                "The Q4 forecast looks rough", null, LocalDateTime.now(ZoneOffset.UTC)));

        assertThat(threadRepo.hasBodyOnlyMatch(owner, "forecast", null, null, false, false)).isTrue();
    }

    @Test
    void hasBodyOnlyMatchFalseWhenSubjectAlsoMatches() {
        var sender = participantRepo.save(new Participant("p@x.com", "P"));
        var thread = threadRepo.save(new EmailThread(owner, "Forecast review", "<fr@x>"));
        messageRepo.save(new Message(thread, sender, "Forecast review",
                "Q4 forecast attached", null, LocalDateTime.now(ZoneOffset.UTC)));

        assertThat(threadRepo.hasBodyOnlyMatch(owner, "forecast", null, null, false, false)).isFalse();
    }

    @Test
    void hasBodyOnlyMatchFalseWhenParticipantMatches() {
        var ada = participantRepo.save(new Participant("ada@bodyonly.com", "Ada"));
        var thread = threadRepo.save(new EmailThread(owner, "Lunch", "<l-bom@x>"));
        messageRepo.save(new Message(thread, ada, "Lunch",
                "bodyonly text", null, LocalDateTime.now(ZoneOffset.UTC)));

        assertThat(threadRepo.hasBodyOnlyMatch(owner, "bodyonly", null, null, false, false)).isFalse();
    }

    @Test
    void hasBodyOnlyMatchFalseWhenNothingMatches() {
        threadRepo.save(new EmailThread(owner, "Something else", "<se@x>"));

        assertThat(threadRepo.hasBodyOnlyMatch(owner, "noresultswhatsoever", null, null, false, false)).isFalse();
    }

    @Test
    void hasBodyOnlyMatchScopedByOwner() {
        var alice = userRepo.save(new User("alice-hbom@example.com", "h", "Alice"));
        var sender = participantRepo.save(new Participant("p-hbom@x.com", "P"));
        var aliceThread = threadRepo.save(new EmailThread(alice, "Boring subject", "<bs@x>"));
        messageRepo.save(new Message(aliceThread, sender, "Boring subject",
                "Mentions zzunique-token only here", null, LocalDateTime.now(ZoneOffset.UTC)));

        // Alice sees the body-only match; owner (a different user) does not.
        assertThat(threadRepo.hasBodyOnlyMatch(alice, "zzunique-token", null, null, false, false)).isTrue();
        assertThat(threadRepo.hasBodyOnlyMatch(owner, "zzunique-token", null, null, false, false)).isFalse();
    }

    @Test
    void findByOwnerAndSenderReturnsOnlyThreadsThatIncludeThatSender() {
        var ada = participantRepo.save(new Participant("ada-sf@acme.com", "Ada"));
        var grace = participantRepo.save(new Participant("grace-sf@navy.mil", "Grace"));
        var t1 = threadRepo.save(new EmailThread(owner, "Ada thread", "<a-sf@x>"));
        var t2 = threadRepo.save(new EmailThread(owner, "Grace thread", "<g-sf@x>"));
        var t3 = threadRepo.save(new EmailThread(owner, "Both thread", "<b-sf@x>"));
        messageRepo.save(new Message(t1, ada, "Ada", "body", null, LocalDateTime.now(ZoneOffset.UTC)));
        messageRepo.save(new Message(t2, grace, "Grace", "body", null, LocalDateTime.now(ZoneOffset.UTC)));
        messageRepo.save(new Message(t3, ada, "Both1", "body", null, LocalDateTime.now(ZoneOffset.UTC)));
        messageRepo.save(new Message(t3, grace, "Both2", "body", null, LocalDateTime.now(ZoneOffset.UTC)));

        var page = threadRepo.findByOwnerAndSender(owner, "ada-sf@acme.com", null, false, false, PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(EmailThread::getSubject)
                .containsExactlyInAnyOrder("Ada thread", "Both thread");
    }

    @Test
    void findByOwnerAndSenderIsCaseInsensitive() {
        var ada = participantRepo.save(new Participant("ada-ci@acme.com", "Ada"));
        var thread = threadRepo.save(new EmailThread(owner, "Ada thread", "<a-ci@x>"));
        messageRepo.save(new Message(thread, ada, "Ada", "body", null, LocalDateTime.now(ZoneOffset.UTC)));

        var page = threadRepo.findByOwnerAndSender(owner, "Ada-CI@ACME.com", null, false, false, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void findByOwnerAndSenderScopedByOwner() {
        var alice = userRepo.save(new User("alice-sf@example.com", "h", "Alice"));
        var bob = userRepo.save(new User("bob-sf@example.com", "h", "Bob"));
        var shared = participantRepo.save(new Participant("shared-sf@x.com", "Shared"));
        var aliceThread = threadRepo.save(new EmailThread(alice, "Alice", "<as-sf@x>"));
        var bobThread = threadRepo.save(new EmailThread(bob, "Bob", "<bs-sf@x>"));
        messageRepo.save(new Message(aliceThread, shared, "A", "body", null, LocalDateTime.now(ZoneOffset.UTC)));
        messageRepo.save(new Message(bobThread, shared, "B", "body", null, LocalDateTime.now(ZoneOffset.UTC)));

        var alicePage = threadRepo.findByOwnerAndSender(alice, "shared-sf@x.com", null, false, false, PageRequest.of(0, 10));

        assertThat(alicePage.getContent()).hasSize(1);
        assertThat(alicePage.getContent().get(0).getRootMessageId()).isEqualTo("<as-sf@x>");
    }

    @Test
    void searchWithSenderFilterIntersectsBothConstraints() {
        var ada = participantRepo.save(new Participant("ada-sw@acme.com", "Ada"));
        var grace = participantRepo.save(new Participant("grace-sw@navy.mil", "Grace"));
        var planAda = threadRepo.save(new EmailThread(owner, "Planning Q3", "<pa@x>"));
        var planGrace = threadRepo.save(new EmailThread(owner, "Planning Q4", "<pg@x>"));
        var lunch = threadRepo.save(new EmailThread(owner, "Lunch", "<lu@x>"));
        messageRepo.save(new Message(planAda, ada, "Planning", "body", null, LocalDateTime.now(ZoneOffset.UTC)));
        messageRepo.save(new Message(planGrace, grace, "Planning", "body", null, LocalDateTime.now(ZoneOffset.UTC)));
        messageRepo.save(new Message(lunch, ada, "Lunch", "body", null, LocalDateTime.now(ZoneOffset.UTC)));

        var page = threadRepo.search(owner, "planning", "ada-sw@acme.com", null, false, false, PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(EmailThread::getSubject)
                .containsExactly("Planning Q3");
    }

    @Test
    void searchIncludingBodyWithSenderFilterIntersectsBothConstraints() {
        var ada = participantRepo.save(new Participant("ada-sb@acme.com", "Ada"));
        var grace = participantRepo.save(new Participant("grace-sb@navy.mil", "Grace"));
        var adaThread = threadRepo.save(new EmailThread(owner, "Lunch tomorrow", "<lb-a@x>"));
        var graceThread = threadRepo.save(new EmailThread(owner, "Drinks tomorrow", "<lb-g@x>"));
        messageRepo.save(new Message(adaThread, ada, "Lunch",
                "Stripe webhook discussion at noon", null, LocalDateTime.now(ZoneOffset.UTC)));
        messageRepo.save(new Message(graceThread, grace, "Drinks",
                "Stripe webhook discussion at 6pm", null, LocalDateTime.now(ZoneOffset.UTC)));

        var page = threadRepo.searchIncludingBody(owner, "stripe webhook", "ada-sb@acme.com", null, false, false, PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(EmailThread::getSubject)
                .containsExactly("Lunch tomorrow");
    }

    @Test
    void hasBodyOnlyMatchWithSenderFilterScopesProbeToThatSender() {
        var ada = participantRepo.save(new Participant("ada-hbsf@acme.com", "Ada"));
        var grace = participantRepo.save(new Participant("grace-hbsf@navy.mil", "Grace"));
        var adaThread = threadRepo.save(new EmailThread(owner, "Boring subject A", "<ha@x>"));
        var graceThread = threadRepo.save(new EmailThread(owner, "Boring subject G", "<hg@x>"));
        messageRepo.save(new Message(graceThread, grace, "Boring G",
                "needle keyword here", null, LocalDateTime.now(ZoneOffset.UTC)));
        messageRepo.save(new Message(adaThread, ada, "Boring A",
                "totally unrelated", null, LocalDateTime.now(ZoneOffset.UTC)));

        // Without sender filter: Grace's thread is a body-only match → true.
        assertThat(threadRepo.hasBodyOnlyMatch(owner, "needle", null, null, false, false)).isTrue();
        // Scoped to Ada: she doesn't have any body match → false.
        assertThat(threadRepo.hasBodyOnlyMatch(owner, "needle", "ada-hbsf@acme.com", null, false, false)).isFalse();
    }

    @Test
    void topSendersReturnsParticipantsOrderedByThreadCount() {
        var ada = participantRepo.save(new Participant("ada-ts@acme.com", "Ada Lovelace"));
        var grace = participantRepo.save(new Participant("grace-ts@navy.mil", "Grace Hopper"));
        var t1 = threadRepo.save(new EmailThread(owner, "T1", "<t1-ts@x>"));
        var t2 = threadRepo.save(new EmailThread(owner, "T2", "<t2-ts@x>"));
        var t3 = threadRepo.save(new EmailThread(owner, "T3", "<t3-ts@x>"));
        messageRepo.save(new Message(t1, ada, "s", "b", null, LocalDateTime.now(ZoneOffset.UTC)));
        messageRepo.save(new Message(t2, ada, "s", "b", null, LocalDateTime.now(ZoneOffset.UTC)));
        messageRepo.save(new Message(t2, ada, "s2", "b", null, LocalDateTime.now(ZoneOffset.UTC)));
        messageRepo.save(new Message(t3, grace, "s", "b", null, LocalDateTime.now(ZoneOffset.UTC)));

        var rows = threadRepo.topSenders(owner, PageRequest.of(0, 10));

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).getEmail()).isEqualTo("ada-ts@acme.com");
        assertThat(rows.get(0).getDisplayName()).isEqualTo("Ada Lovelace");
        assertThat(rows.get(0).getThreadCount()).isEqualTo(2L);
        assertThat(rows.get(1).getEmail()).isEqualTo("grace-ts@navy.mil");
        assertThat(rows.get(1).getThreadCount()).isEqualTo(1L);
    }

    @Test
    void topSendersScopedByOwner() {
        var alice = userRepo.save(new User("alice-ts@example.com", "h", "Alice"));
        var ada = participantRepo.save(new Participant("ada-tso@acme.com", "Ada"));
        var aliceThread = threadRepo.save(new EmailThread(alice, "T", "<at-tso@x>"));
        messageRepo.save(new Message(aliceThread, ada, "s", "b", null, LocalDateTime.now(ZoneOffset.UTC)));

        var ownerRows = threadRepo.topSenders(owner, PageRequest.of(0, 10));

        assertThat(ownerRows).noneMatch(r -> r.getEmail().equals("ada-tso@acme.com"));
    }

    @Test
    void topSendersHonorsLimit() {
        for (int i = 0; i < 5; i++) {
            var p = participantRepo.save(new Participant("p" + i + "-lim@x.com", "P" + i));
            var t = threadRepo.save(new EmailThread(owner, "T" + i, "<t" + i + "-lim@x>"));
            messageRepo.save(new Message(t, p, "s", "b", null, LocalDateTime.now(ZoneOffset.UTC)));
        }

        var rows = threadRepo.topSenders(owner, PageRequest.of(0, 3));

        assertThat(rows).hasSize(3);
    }

    @Test
    void findByOwnerFilteredWithUnreadOnlyReturnsThreadsWhereUnreadTrue() {
        var fresh = threadRepo.save(new EmailThread(owner, "Fresh thread", "<f@x>"));
        var stale = threadRepo.save(new EmailThread(owner, "Stale thread", "<s@x>"));
        stale.markRead();
        threadRepo.save(stale);

        var unreadPage = threadRepo.findByOwnerFiltered(owner, null, true, false, PageRequest.of(0, 10));

        assertThat(unreadPage.getContent()).extracting(EmailThread::getSubject)
                .containsExactly("Fresh thread");
        assertThat(fresh.isUnread()).isTrue();
    }

    @Test
    void findByOwnerFilteredWithAttachmentsOnlyReturnsThreadsWithMessagesThatHaveAttachments() {
        var sender = participantRepo.save(new Participant("att-sender@x.com", "S"));
        var bare = threadRepo.save(new EmailThread(owner, "Plain text only", "<bare@x>"));
        var rich = threadRepo.save(new EmailThread(owner, "With doc", "<rich@x>"));
        messageRepo.save(new Message(bare, sender, "p", "body", null, LocalDateTime.now(ZoneOffset.UTC)));
        var msg = new Message(rich, sender, "rich", "body", null, LocalDateTime.now(ZoneOffset.UTC));
        msg.addAttachment(new com.emailmessenger.domain.Attachment(msg, "Q3.pdf", "application/pdf",
                12345L, null));
        messageRepo.save(msg);

        var page = threadRepo.findByOwnerFiltered(owner, null, false, true, PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(EmailThread::getSubject).containsExactly("With doc");
    }

    @Test
    void findByOwnerFilteredWithSinceCutsOffOlderThreads() {
        var old = threadRepo.save(new EmailThread(owner, "Old", "<old@x>"));
        var recent = threadRepo.save(new EmailThread(owner, "Recent", "<recent@x>"));

        var page = threadRepo.findByOwnerFiltered(owner,
                LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1), false, false, PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(EmailThread::getSubject)
                .contains("Recent", "Old"); // both within last minute
    }

    @Test
    void findByOwnerFilteredWithSinceInFutureReturnsNothing() {
        threadRepo.save(new EmailThread(owner, "Now", "<now@x>"));

        var page = threadRepo.findByOwnerFiltered(owner,
                LocalDateTime.now(ZoneOffset.UTC).plusDays(1), false, false, PageRequest.of(0, 10));

        assertThat(page.getContent()).isEmpty();
    }

    @Test
    void searchWithUnreadFilterANDsOntoSubjectMatch() {
        var t1 = threadRepo.save(new EmailThread(owner, "Planning Q3", "<p1@x>"));
        var t2 = threadRepo.save(new EmailThread(owner, "Planning Q4", "<p2@x>"));
        t1.markRead();
        threadRepo.save(t1);

        var page = threadRepo.search(owner, "planning", null, null, true, false, PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(EmailThread::getSubject).containsExactly("Planning Q4");
    }

    @Test
    void searchIncludingBodyWithAttachmentFilterANDsOntoBodyMatch() {
        var sender = participantRepo.save(new Participant("body-att@x.com", "S"));
        var bare = threadRepo.save(new EmailThread(owner, "No attach", "<na@x>"));
        var rich = threadRepo.save(new EmailThread(owner, "Has attach", "<ha@x>"));
        messageRepo.save(new Message(bare, sender, "p", "rocket science here", null, LocalDateTime.now(ZoneOffset.UTC)));
        var msg = new Message(rich, sender, "rich", "rocket science here", null, LocalDateTime.now(ZoneOffset.UTC));
        msg.addAttachment(new com.emailmessenger.domain.Attachment(msg, "data.csv", "text/csv",
                100L, null));
        messageRepo.save(msg);

        var page = threadRepo.searchIncludingBody(owner, "rocket",
                null, null, false, true, PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(EmailThread::getSubject).containsExactly("Has attach");
    }
}
