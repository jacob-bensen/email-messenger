package com.emailmessenger.service;

import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.domain.Message;
import com.emailmessenger.domain.Participant;
import com.emailmessenger.domain.User;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationServiceTest {

    private final ConversationService svc = new ConversationService(new IMTransformService());

    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2025, 1, 1, 10, 0);
    private static final User OWNER = new User("owner@test.com", "h", null);

    private EmailThread thread(String subject) {
        return new EmailThread(OWNER, subject, "<root@test>");
    }

    private Message message(EmailThread t, Participant sender, String bodyPlain, int minuteOffset) {
        return new Message(t, sender, "Subject", bodyPlain, null, BASE_TIME.plusMinutes(minuteOffset));
    }

    // --- bubble run grouping ---

    @Test
    void emptyThreadProducesNoRuns() {
        Conversation conv = svc.buildConversation(thread("Empty"));
        assertThat(conv.runs()).isEmpty();
    }

    @Test
    void singleMessageProducesOneBubbleRun() {
        EmailThread t = thread("Test");
        Participant alice = new Participant("alice@test.com", "Alice");
        t.addMessage(message(t, alice, "Hello!", 0));

        Conversation conv = svc.buildConversation(t);

        assertThat(conv.runs()).hasSize(1);
        assertThat(conv.runs().get(0).sender().email()).isEqualTo("alice@test.com");
        assertThat(conv.runs().get(0).messages()).hasSize(1);
    }

    @Test
    void consecutiveSameSenderGroupedInOneBubbleRun() {
        EmailThread t = thread("Test");
        Participant alice = new Participant("alice@test.com", "Alice");
        t.addMessage(message(t, alice, "Message 1", 0));
        t.addMessage(message(t, alice, "Message 2", 1));
        t.addMessage(message(t, alice, "Message 3", 2));

        Conversation conv = svc.buildConversation(t);

        assertThat(conv.runs()).hasSize(1);
        assertThat(conv.runs().get(0).messages()).hasSize(3);
    }

    @Test
    void differentSendersProduceMultipleBubbleRuns() {
        EmailThread t = thread("Test");
        Participant alice = new Participant("alice@test.com", "Alice");
        Participant bob = new Participant("bob@test.com", "Bob");
        t.addMessage(message(t, alice, "Hi Bob", 0));
        t.addMessage(message(t, bob, "Hey Alice", 1));
        t.addMessage(message(t, alice, "How are you?", 2));

        Conversation conv = svc.buildConversation(t);

        assertThat(conv.runs()).hasSize(3);
        assertThat(conv.runs().get(0).sender().email()).isEqualTo("alice@test.com");
        assertThat(conv.runs().get(1).sender().email()).isEqualTo("bob@test.com");
        assertThat(conv.runs().get(2).sender().email()).isEqualTo("alice@test.com");
    }

    @Test
    void mixedGroupsProduceCorrectRunCounts() {
        EmailThread t = thread("Test");
        Participant alice = new Participant("alice@test.com", "Alice");
        Participant bob = new Participant("bob@test.com", "Bob");
        // alice x2, bob x3, alice x1 → 3 runs
        t.addMessage(message(t, alice, "A1", 0));
        t.addMessage(message(t, alice, "A2", 1));
        t.addMessage(message(t, bob, "B1", 2));
        t.addMessage(message(t, bob, "B2", 3));
        t.addMessage(message(t, bob, "B3", 4));
        t.addMessage(message(t, alice, "A3", 5));

        Conversation conv = svc.buildConversation(t);

        assertThat(conv.runs()).hasSize(3);
        assertThat(conv.runs().get(0).messages()).hasSize(2);
        assertThat(conv.runs().get(1).messages()).hasSize(3);
        assertThat(conv.runs().get(2).messages()).hasSize(1);
    }

    // --- body HTML transform ---

    @Test
    void plainBodyIsTransformedToHtml() {
        EmailThread t = thread("Test");
        Participant alice = new Participant("alice@test.com", "Alice");
        t.addMessage(message(t, alice, "Hello **world**!", 0));

        Conversation conv = svc.buildConversation(t);

        String bodyHtml = conv.runs().get(0).messages().get(0).bodyHtml();
        assertThat(bodyHtml).contains("<strong>world</strong>");
    }

    @Test
    void htmlBodyPassesThroughWithoutTransform() {
        EmailThread t = thread("Test");
        Participant alice = new Participant("alice@test.com", "Alice");
        Message msg = new Message(t, alice, "Subject", "plain text",
            "<p>Already <b>HTML</b></p>", BASE_TIME);
        t.addMessage(msg);

        Conversation conv = svc.buildConversation(t);

        String bodyHtml = conv.runs().get(0).messages().get(0).bodyHtml();
        assertThat(bodyHtml).contains("Already");
        assertThat(bodyHtml).contains("<b>HTML</b>");
    }

    // --- HTML sanitization (XSS prevention) ---

    @Test
    void scriptTagsStrippedFromHtmlBody() {
        EmailThread t = thread("XSS");
        Participant alice = new Participant("alice@test.com", "Alice");
        Message msg = new Message(t, alice, "Subject", "plain",
            "<p>Hello</p><script>alert('xss')</script>", BASE_TIME);
        t.addMessage(msg);

        String bodyHtml = svc.buildConversation(t).runs().get(0).messages().get(0).bodyHtml();

        assertThat(bodyHtml).doesNotContain("<script>");
        assertThat(bodyHtml).doesNotContain("alert");
        assertThat(bodyHtml).contains("Hello");
    }

    @Test
    void inlineEventHandlersStrippedFromHtmlBody() {
        EmailThread t = thread("XSS");
        Participant alice = new Participant("alice@test.com", "Alice");
        Message msg = new Message(t, alice, "Subject", "plain",
            "<a href=\"#\" onclick=\"steal()\">click me</a>", BASE_TIME);
        t.addMessage(msg);

        String bodyHtml = svc.buildConversation(t).runs().get(0).messages().get(0).bodyHtml();

        assertThat(bodyHtml).doesNotContain("onclick");
        assertThat(bodyHtml).contains("click me");
    }

    @Test
    void javascriptLinksStrippedFromHtmlBody() {
        EmailThread t = thread("XSS");
        Participant alice = new Participant("alice@test.com", "Alice");
        Message msg = new Message(t, alice, "Subject", "plain",
            "<a href=\"javascript:void(0)\">danger</a>", BASE_TIME);
        t.addMessage(msg);

        String bodyHtml = svc.buildConversation(t).runs().get(0).messages().get(0).bodyHtml();

        assertThat(bodyHtml).doesNotContain("javascript:");
    }

    @Test
    void safeHtmlElementsPreservedAfterSanitization() {
        EmailThread t = thread("Safe");
        Participant alice = new Participant("alice@test.com", "Alice");
        Message msg = new Message(t, alice, "Subject", "plain",
            "<p><strong>Bold</strong> and <em>italic</em></p>", BASE_TIME);
        t.addMessage(msg);

        String bodyHtml = svc.buildConversation(t).runs().get(0).messages().get(0).bodyHtml();

        assertThat(bodyHtml).contains("<strong>Bold</strong>");
        assertThat(bodyHtml).contains("<em>italic</em>");
    }

    // --- BubbleRun.date() ---

    @Test
    void bubbleRunDateReturnsLocalDateOfFirstMessage() {
        EmailThread t = thread("Test");
        Participant alice = new Participant("alice@test.com", "Alice");
        LocalDateTime ts = LocalDateTime.of(2025, 6, 15, 9, 30);
        t.addMessage(new Message(t, alice, "Subject", "Hello", null, ts));

        Conversation conv = svc.buildConversation(t);

        assertThat(conv.runs().get(0).date())
            .isEqualTo(ts.toLocalDate());
    }

    @Test
    void bubbleRunDateIsNullWhenFirstMessageHasNullSentAt() {
        EmailThread t = thread("Test");
        Participant alice = new Participant("alice@test.com", "Alice");
        t.addMessage(new Message(t, alice, "Subject", "Hello", null, null));

        Conversation conv = svc.buildConversation(t);

        assertThat(conv.runs().get(0).date()).isNull();
    }

    @Test
    void separateRunsOnDifferentDaysHaveDifferentDates() {
        EmailThread t = thread("Test");
        Participant alice = new Participant("alice@test.com", "Alice");
        Participant bob   = new Participant("bob@test.com", "Bob");
        LocalDateTime day1 = LocalDateTime.of(2025, 6, 15, 9, 0);
        LocalDateTime day2 = LocalDateTime.of(2025, 6, 16, 10, 0);
        t.addMessage(new Message(t, alice, "Subject", "Day 1", null, day1));
        t.addMessage(new Message(t, bob,   "Subject", "Day 2", null, day2));

        Conversation conv = svc.buildConversation(t);

        assertThat(conv.runs().get(0).date()).isNotEqualTo(conv.runs().get(1).date());
    }

    @Test
    void outboundMessageProducesOutboundRunSeparateFromInbound() {
        EmailThread t = thread("Test");
        Participant them = new Participant("them@test.com", "Them");
        Participant me = new Participant("me@test.com", "Me");
        t.addMessage(message(t, them, "Hi", 0));
        Message reply = message(t, me, "Hello back", 1);
        reply.markOutbound();
        t.addMessage(reply);

        Conversation conv = svc.buildConversation(t);

        assertThat(conv.runs()).hasSize(2);
        assertThat(conv.runs().get(0).outbound()).isFalse();
        assertThat(conv.runs().get(1).outbound()).isTrue();
    }

    // --- sender conversation (cross-thread chat) ---

    @Test
    void senderConversationGroupsByThreadAndBadgesEachRun() {
        Participant ada = new Participant("ada@acme.com", "Ada Lovelace");
        EmailThread t1 = thread("Invoice question");
        EmailThread t2 = thread("Project kickoff");
        Message m1 = message(t1, ada, "About the invoice", 0);
        Message m2 = message(t1, ada, "Following up", 1);
        Message m3 = message(t2, ada, "Kickoff is Monday", 2);

        SenderConversation conv = svc.buildSenderConversation(List.of(m1, m2, m3), "ada@acme.com");

        assertThat(conv.sender().email()).isEqualTo("ada@acme.com");
        assertThat(conv.runs()).hasSize(2);
        assertThat(conv.runs().get(0).messages()).hasSize(2);
        assertThat(conv.runs().get(0).thread().subject()).isEqualTo("Invoice question");
        assertThat(conv.runs().get(1).messages()).hasSize(1);
        assertThat(conv.runs().get(1).thread().subject()).isEqualTo("Project kickoff");
        assertThat(conv.threadCount()).isEqualTo(2);
        assertThat(conv.messageCount()).isEqualTo(3);
    }

    @Test
    void senderConversationCountsRecurringThreadOnce() {
        Participant ada = new Participant("ada@acme.com", "Ada");
        EmailThread t1 = thread("Thread A");
        EmailThread t2 = thread("Thread B");
        // A, then B, then back to A → 3 runs but only 2 distinct threads
        SenderConversation conv = svc.buildSenderConversation(List.of(
            message(t1, ada, "A1", 0),
            message(t2, ada, "B1", 1),
            message(t1, ada, "A2", 2)), "ada@acme.com");

        assertThat(conv.runs()).hasSize(3);
        assertThat(conv.threadCount()).isEqualTo(2);
    }

    @Test
    void senderConversationInterleavesOwnOutboundRepliesAsYou() {
        Participant ada = new Participant("ada@acme.com", "Ada");
        Participant me = new Participant("me@acme.com", "Me");
        EmailThread t = thread("Invoice");
        Message inbound = message(t, ada, "Can you send the invoice?", 0);
        Message reply = message(t, me, "Sure, attached.", 1);
        reply.markOutbound();

        SenderConversation conv = svc.buildSenderConversation(List.of(inbound, reply), "ada@acme.com");

        // Header is the correspondent, not the outbound side.
        assertThat(conv.sender().email()).isEqualTo("ada@acme.com");
        assertThat(conv.runs()).hasSize(2);
        assertThat(conv.runs().get(0).outbound()).isFalse();
        assertThat(conv.runs().get(1).outbound()).isTrue();
        assertThat(conv.messageCount()).isEqualTo(2);
    }

    @Test
    void htmlQuotedReplyBlockIsStrippedFromBubble() {
        EmailThread t = thread("Re: Hello");
        Participant ada = new Participant("ada@acme.com", "Ada");
        Message msg = new Message(t, ada, "Re: Hello", "plain",
            "<p>Thanks!</p><blockquote>On Mon you wrote: the original message</blockquote>", BASE_TIME);
        t.addMessage(msg);

        String bodyHtml = svc.buildConversation(t).runs().get(0).messages().get(0).bodyHtml();
        assertThat(bodyHtml).contains("Thanks!");
        assertThat(bodyHtml).doesNotContain("the original message");
    }

    @Test
    void senderConversationOfEmptyListHasNoRuns() {
        SenderConversation conv = svc.buildSenderConversation(List.of(), "ada@acme.com");

        assertThat(conv.runs()).isEmpty();
        assertThat(conv.threadCount()).isEqualTo(0);
        assertThat(conv.messageCount()).isEqualTo(0);
        assertThat(conv.sender()).isNull();
    }

    @Test
    void quotedRepliesStrippedBeforeRendering() {
        EmailThread t = thread("Test");
        Participant alice = new Participant("alice@test.com", "Alice");
        String body = "My reply.\n\nOn Mon, Jun 5 wrote:\n> Old content";
        t.addMessage(message(t, alice, body, 0));

        Conversation conv = svc.buildConversation(t);

        String bodyHtml = conv.runs().get(0).messages().get(0).bodyHtml();
        assertThat(bodyHtml).doesNotContain("Old content");
        assertThat(bodyHtml).contains("My reply");
    }
}
